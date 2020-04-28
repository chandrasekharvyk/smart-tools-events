package com.mrll.javelin.api.smarttools.service;

import com.google.common.base.Stopwatch;
import com.mrll.javelin.api.smarttools.delegate.AzureBlobDownloadDelegate;
import com.mrll.javelin.api.smarttools.delegate.CategorizationDelegate;
import com.mrll.javelin.api.smarttools.delegate.ChecklistMapperDelegate;
import com.mrll.javelin.api.smarttools.delegate.MetadataDelegate;
import com.mrll.javelin.api.smarttools.exception.FatalSmartSortException;
import com.mrll.javelin.api.smarttools.model.delegates.*;
import com.mrll.javelin.api.smarttools.mongo.entity.SmartDoc;
import com.mrll.javelin.api.smarttools.model.delegates.Metadata;
import com.mrll.javelin.api.smarttools.mongo.entity.SmartDoc;
import com.mrll.javelin.api.smarttools.publisher.AuditingPublisher;
import com.mrll.javelin.api.smarttools.publisher.CategorizationPublisher;
import com.mrll.javelin.api.smarttools.publisher.model.CategorizationEvent;
import com.mrll.javelin.api.smarttools.service.SmartDocService;
import com.mrll.javelin.cmcs.domain.MetadataType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class CategorizationService {
    private static final String OCR_FORM = "OCR";

    private AzureBlobDownloadDelegate azureBlobDownloadDelegate;
    private MetadataDelegate metadataDelegate;
    private CategorizationDelegate categorizationDelegate;
    private ChecklistMapperDelegate checklistMapperDelegate;
    private SmartDocService smartDocService;
    private CategorizationPublisher categorizationPublisher;
    private AuditingPublisher auditingPublisher;
    private int PAGE_WORDS = 600;
    private int PAGES = 50;
    private String XML_PAGE = "PAGE";
    private final float cutOffPrediction = 0.6f;
    private final int MAX_FOLDER_SUGGESTION = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(CategorizationService.class);

    @Autowired
    public CategorizationService(AzureBlobDownloadDelegate azureBlobDownloadDelegate,
                                 MetadataDelegate metadataDelegate,
                                 CategorizationDelegate categorizationDelegate,
                                 ChecklistMapperDelegate checklistMapperDelegate,
                                 SmartDocService smartDocService,
                                 CategorizationPublisher categorizationPublisher, AuditingPublisher auditingPublisher) {
        this.azureBlobDownloadDelegate = azureBlobDownloadDelegate;
        this.metadataDelegate = metadataDelegate;
        this.categorizationDelegate = categorizationDelegate;
        this.smartDocService = smartDocService;
        this.categorizationPublisher = categorizationPublisher;
        this.checklistMapperDelegate = checklistMapperDelegate;
        this.auditingPublisher = auditingPublisher;
    }

    public SmartDoc findCategoriesAndSuggestions(final MetadataUpdateEvent event) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        LOGGER.info("method=findCategoriesAndSuggestions trace=smartToolsProcessing message=start event={}", event);
        List<Metadata> metaDatas = metadataDelegate.getMultipleMetadata(event.getProjectId(), event.getMetadataIds());
        SmartDoc smartDoc = findDocCategory(event, metaDatas);

        HashMap<String, FolderInfo> sandboxFolderInfo = getSandboxFolderStructures(event);
        HashMap<String, ChecklistMappingResponse> checklistMappingResponseHashMap = getFolderSuggestion(event, sandboxFolderInfo);

        List<String> matchingFolderSortedPredictions = compareCategoriesWithLabels(checklistMappingResponseHashMap, smartDoc);
        List<FolderSuggestion> folderSuggestions = new ArrayList<>();

        for (String matchingFolder : matchingFolderSortedPredictions) {
            folderSuggestions.add(sandboxFolderInfo.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getPath().equals(matchingFolder))
                    .map(Map.Entry::getValue)
                    .map(it -> it.getFolderSuggestion())
                    .collect(Collectors.toList()).get(0)); //Will never be empty list since we match it.
        }

        smartDoc.setSuggestedFolders(folderSuggestions);
        LOGGER.info("method=createSmartDoc trace=smartToolsProcessing message=saveCategoriesAndSuggestions projectId={} metadataId={} category={} folderSuggestions={}",
                event.getProjectId(), event.getMetadataIds().get(0), smartDoc.getCategories(), folderSuggestions);

        smartDocService.createSmartDoc(smartDoc.getProjectId(), smartDoc.getMetadataId(), smartDoc.getBlobId(), smartDoc.getCategories(), smartDoc.getModelVersion(), folderSuggestions);
        categorizationPublisher.publishCategorizationEvent(
                new CategorizationEvent().setProjectId(event.getProjectId())
                        .setMetadataId(smartDoc.getMetadataId())
                        .setParentId(metaDatas.get(0).getParentId())
                        .setPredictions(smartDoc.getCategories())
                        .setFolderSuggestions(folderSuggestions));

        auditingPublisher.publishAuditCategories(smartDoc, null);
        stopwatch.stop();
        LOGGER.info("method=findCategoriesAndSuggestions trace=smartToolsProcessing message=done projectId={} elapsedTime={}", event.getProjectId(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return smartDoc;
    }

    public SmartDoc findDocCategory(final MetadataUpdateEvent event, List<Metadata> metadatas) {
        LOGGER.info("method=findDocCategory trace=smartToolsProcessing message=start event={}", event);
        Stopwatch stopwatch = null;
        for (Metadata metadata : metadatas) {
            if (metadata.getFileroomType().equals(MetadataType.SANDBOX)) {
                LOGGER.info("method=findDocCategory trace=smartToolsProcessing message=startingCategorizationForDoc metadata={}", metadata);

                stopwatch = Stopwatch.createStarted();

                BlobDetail blobDetail = getRenditionFormInfo(metadata.getBlobDetails(), OCR_FORM);

                LOGGER.info("method=findDocCategory trace=smartToolsProcessing message=retrievedBlobDetail blobDetail={}", blobDetail);

                if (blobDetail == null)
                {
                    LOGGER.error("method=findDocCategory trace=smartToolsProcessing errorMessage=unableToFindOcrRenditionForDocument  metadataId={}", metadata.getId());
                    throw new FatalSmartSortException("Unable to find the OCR rendition for document with metadataId " + metadata.getId());
                }

                String ocrContent;

                try {
                    InputStream blob = downloadBlob(metadata, blobDetail);

                    if (blob == null) {
                        LOGGER.error("method=findDocCategory trace=smartToolsProcessing errorMessage=failedToFindOcrBlobForDocument  metadataId={}", metadata.getId());
                        throw new FatalSmartSortException("failed to find OCR blob for document with metadataId " + metadata.getId());
                    }

                    ocrContent = IOUtils.toString(blob, "UTF-8");

                } catch (IOException e) {
                    LOGGER.error("method=findDocCategory trace=smartToolsProcessing errorMessage=failedToGetOcrContent  metadataId={} blobId={}", metadata.getId(), blobDetail.getBlobId(), e);
                    throw new FatalSmartSortException("failed to find OCR blob for document with metadataId " + metadata.getId(), e);
                }

                if (ocrContent == null) {
                    LOGGER.error("method=findDocCategory trace=smartToolsProcessing errorMessage=ocrContentIsNull  metadataId " + metadata.getId());
                    throw new FatalSmartSortException("Unable to download the OCR rendition for document with metadataId " + metadata.getId());
                }

                //TODO: Remove this comment/latest

                String reducedOcr = reduceOcr(ocrContent, metadata.getId());

                CategorizationResponse response =
                        categorizationDelegate.sendOCRData(event.getProjectId(), metadata.getId(), reducedOcr, metadata.getName());

                stopwatch.stop();
                LOGGER.info("method=findDocCategory trace=smartToolsProcessing message=done projectId={} metadataId={} category={} elapsedTime={}",
                        event.getProjectId(), metadata.getId(), response.toString(), stopwatch.elapsed(TimeUnit.MILLISECONDS));

                SmartDoc smartDoc = new SmartDoc().setProjectId(event.getProjectId())
                        .setMetadataId(metadata.getId())
                        .setBlobId(blobDetail.getBlobId())
                        .setCategories(response.getPredictions())
                        .setModelVersion(response.getModelVersion());
                return smartDoc;
            }

        }
        return null;
    }


    /**
     * Filter matching categories between doc content categorization and checklist mapper
     *
     * @param checklistMappingResponseHashMap
     * @param smartDoc
     * @return
     */
    private List<String> compareCategoriesWithLabels(HashMap<String, ChecklistMappingResponse> checklistMappingResponseHashMap, SmartDoc smartDoc) {
        LOGGER.info("method=compareCategoriesWithLabels trace=smartToolsProcessing message=start");
        List<String> folderNames = new ArrayList<>();
        List<String> categoryPredictions = smartDoc.getCategories().stream()
                .map(x -> x.getLabel().toLowerCase())
                .collect(Collectors.toList());
        Map<String, Integer> folderNamesMatchingMap = new HashMap<>();
        for (Map.Entry<String, ChecklistMappingResponse> entry : checklistMappingResponseHashMap.entrySet()) {
            int matchingLabels = entry.getValue()
                    .getPredictions()
                    .stream()
                    .map(x -> x.getLabel().toLowerCase()).collect(Collectors.toList())
                    .stream()
                    .filter(categoryPredictions::contains)
                    .collect(Collectors.toList()).size();
            folderNamesMatchingMap.put(entry.getKey(), matchingLabels);
        }
        LOGGER.info("method=compareCategoriesWithLabels trace=smartToolsProcessing folderLabelMatches={} projectId={} metadataId={}",
                folderNamesMatchingMap, smartDoc.getProjectId(), smartDoc.getMetadataId());

        folderNames.addAll(folderNamesMatchingMap.entrySet()
                .stream()
                .filter(x -> x.getValue() >= 1)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(MAX_FOLDER_SUGGESTION)
                .map(Map.Entry::getKey).collect(Collectors.toList()));
        LOGGER.info("method=compareCategoriesWithLabels trace=smartToolsProcessing message=done folderNames={} projectId={} metadataId={}",
                folderNames, smartDoc.getProjectId(), smartDoc.getMetadataId());
        return folderNames;
    }


    public void deleteDocCategory(final MetadataUpdateEvent event) {
        List<SmartDoc> smartDocs = smartDocService.deleteSmartDoc(event.getProjectId(), event.getMetadataIds());
        LOGGER.info("method=deleteDocCategory trace=smartToolsProcessing message=start details=Deleted smart docs count {}", smartDocs.size());
    }

    private BlobDetail getRenditionFormInfo(List<BlobDetail> blobDetails, String formType) {

        LOGGER.info("method=getRenditionFormInfo trace=smartToolsProcessing message=retrieving blob details - blobDetails={}", blobDetails);

        for (BlobDetail blobDetail : blobDetails) {

            LOGGER.info("method=getRenditionFormInfo trace=smartToolsProcessing message=checking blob detail - blobDetail={}", blobDetail);

            if (blobDetail.getProcessName().equals(formType)) {
                LOGGER.info("method=getRenditionFormInfo trace=smartToolsProcessing message=done formType={} found - blobDetail={}", formType, blobDetail);
                return blobDetail;
            }
        }

        LOGGER.error("method=getPdfcFormInfo trace=smartToolsProcessing  errorMessage=unableToLocateBlobDetail blobDetails={}", blobDetails);

        return null;
    }

    private InputStream downloadBlob(Metadata metadata, BlobDetail blobDetail) throws IOException {
        return azureBlobDownloadDelegate.getDocument(metadata.getProjectId(), blobDetail.getBlobId());
    }

    /**
     * Reduce OCR Text size that will take out xml tags and has max of PAGE_WORDS * PAGES words. if exeeds then send PAGE_WORDS words per each page.
     *
     * @param octText
     * @return
     */
    private String reduceOcr(String octText, String metadataId) {
        LOGGER.info("method=reduceOcr trace=smartToolsProcessing message=start");
        StringBuffer stringBuffer = new StringBuffer();
        try {
            URL styleSheetUrl = getClass().getClassLoader().getResource("onlytext.xsl");
            StreamSource stylesSource = new StreamSource(new File(styleSheetUrl.toURI()));
            Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(stylesSource);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new InputSource(new StringReader(octText)));
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            if (stringWriter.toString().split(" ").length <= (PAGE_WORDS * PAGES)) {
                LOGGER.info("method=reduceOcr tripped xml string has less than 30000 words for metadataId={}", metadataId);
                return stringWriter.toString();
            }
            Object[] stringTokenPages = stringWriter.toString().split(XML_PAGE);
            for (int i = 0; i < stringTokenPages.length; i++) {
                Object[] tokensPerPage = stringTokenPages[i].toString().split(" ");
                if (tokensPerPage.length > PAGE_WORDS) {
                    LOGGER.info("method=reduceOcr,string has more page words for metadataId={}", metadataId);
                    Object[] tokens = ArrayUtils.subarray(tokensPerPage, 0, PAGE_WORDS);
                    stringBuffer.append(StringUtils.join(tokens, ' '));
                } else {
                    stringBuffer.append(StringUtils.join(tokensPerPage, ' '));
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        LOGGER.info("method=reduceOcr trace=smartToolsProcessing message=done");
        return stringBuffer.toString();
    }

    public HashMap<String, ChecklistMappingResponse> getFolderSuggestion(MetadataUpdateEvent event, HashMap<String, FolderInfo> sandboxFolderInfo) {
        LOGGER.info("method=getFolderSuggestion trace=smartToolsProcessing message=start");
        List<String> folderPaths = sandboxFolderInfo.entrySet().stream().map(res -> res.getValue().getPath()).collect(Collectors.toList());
        HashMap<String, ChecklistMappingResponse> checklistMappingResponseHashMap = checklistMapperDelegate.getTaxonomyMappings(event, folderPaths);
        LOGGER.info("method=getFolderSuggestion trace=smartToolsProcessing message=done");
        return checklistMappingResponseHashMap;
    }

    private HashMap<String, FolderInfo> getSandboxFolderStructures(final MetadataUpdateEvent event) {
        LOGGER.info("method=getSandboxFolderStructures trace=smartToolsProcessing message=start");
        String sandboxID = metadataDelegate.getSandboxFileRoomID(event.getProjectId(), event.getMetadataIds());
        List<Metadata> sandboxMetadata = metadataDelegate.getSandboxFolders(event.getProjectId(), sandboxID, event.getMetadataIds());

        Map<String, Metadata> metadataIDMap = sandboxMetadata.stream().collect(
                Collectors.toMap(x -> x.getId(), x -> x));

        Iterator metadataIterator = metadataIDMap.entrySet().iterator();
        HashMap<String, FolderInfo> foldersHashMap = new HashMap<>();
        //For each metadataID it iterates to find parent in the tree
        while (metadataIterator.hasNext()) {
            Map.Entry mapElement = (Map.Entry) metadataIterator.next();
            String metadataId = ((Metadata) mapElement.getValue()).getId();
            foldersHashMap.putAll(getParents(metadataIDMap, metadataId, sandboxID, new HashMap<String, FolderInfo>()));
        }
        LOGGER.info("method=uniqueSandboxFlatFolders trace=smartToolsProcessing message=done project={} metadataIds={} folders={}", event.getProjectId(), event.getMetadataIds(), foldersHashMap);
        return foldersHashMap;
    }

    /**
     * Get parents list until it reaches sandbox
     * returns [fol1,abc,test] if test is last child of fol1 given fol1/abc/test order.
     *
     * @param metadataHashMap
     * @param id
     * @param sandboxID
     * @param foldersHashMap
     * @return
     */
    private HashMap<String, FolderInfo> getParents(Map<String, Metadata> metadataHashMap, String id, String sandboxID, HashMap<String, FolderInfo> foldersHashMap) {
        if (!id.equalsIgnoreCase(sandboxID)) {
            String parentId = metadataHashMap.get(id).getParentId();
            FolderInfo folderInfo = new FolderInfo().setId(id)
                    .setName(metadataHashMap.get(id).getName())
                    .setIndex(metadataHashMap.get(id).getDisplayIndex())
                    .setParentId(parentId);
            foldersHashMap.put(id, folderInfo);
            foldersHashMap.get(id).prefixPath(metadataHashMap.get(id).getName());

            FolderInfo childFolder = foldersHashMap.entrySet()
                    .stream()
                    .filter(res -> res.getValue().getParentId().equals(id))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
            if (childFolder != null) {
                //Have Parent appended to all child folders
                //childFolder.prefixPath(metadataHashMap.get(id).getName());
                foldersHashMap.entrySet().stream()
                        .filter(res -> !res.getValue().getId().equals(id))
                        .map(Map.Entry::getValue).map(it -> it.prefixPath(metadataHashMap.get(id).getName())).collect(Collectors.toList());
            }

            if (!parentId.equalsIgnoreCase(sandboxID)) {
                getParents(metadataHashMap, parentId, sandboxID, foldersHashMap);
            }
        }

        return foldersHashMap;
    }

}

