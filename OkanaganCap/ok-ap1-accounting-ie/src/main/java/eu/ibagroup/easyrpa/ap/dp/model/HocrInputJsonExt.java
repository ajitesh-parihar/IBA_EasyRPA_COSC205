package eu.ibagroup.easyrpa.ap.dp.model;

import eu.ibagroup.easyrpa.ap.dp.context.DocumentContext;
import eu.ibagroup.easyrpa.ap.dp.context.HasStorage;
import eu.ibagroup.easyrpa.ap.dp.entity.DpDocument;
import eu.ibagroup.easyrpa.engine.task.ml.MlTaskData;
import eu.ibagroup.easyrpa.engine.task.ocr.*;
import eu.ibagroup.easyrpa.ocr.recognition.converter.hocr.Hocr2Json2JsonConverter;
import eu.ibagroup.easyrpa.ocr.recognition.converter.hocr.Hocr2JsonProcessing;
import eu.ibagroup.easyrpa.utils.JsonUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class HocrInputJsonExt extends StorageContextJson implements InputJson {

    public static final String IMAGES = "images";

    public static final String CONTENT = "content";

    public static final String TESSINPUT = "tessinput";

    public static final String JSON_SRC = "json_src";

    public static final String JSON = "json";

    public static final String DIMENSIONS = "dimensions";

    public static final String WIDTH = "width";

    public static final String HEIGHT = "height";

    public static final String PAGE = "page";

    public static final String FORMAT = "format";

    public static final String DATA = "data";

    public static final String HOCR = "hocr";

    public static final String HOCR_SRC = "hocr_src";

    public static final String TEXT = "text";

    public static final String TEXT_SRC = "text_src";

    public static final String DOCUMENT_ID = "documentId";

    public static final String PAGES = "pages";

    public static final String DOCUMENTS = "documents";

    private HocrInputJsonExt(HasStorage hasStorage) {
        super(hasStorage);
    }

    private HocrInputJsonExt(Map<String, Object> json, HasStorage hasStorage) {
        super(json, hasStorage);
    }

    public static HocrInputJsonExt empty(HasStorage hasStorage) {
        return new HocrInputJsonExt(hasStorage);
    }

    public static HocrInputJsonExt fromJson(Map<String, Object> json, HasStorage hasStorage) {
        return new HocrInputJsonExt(json, hasStorage);
    }

    public static HocrInputJsonExt fromInputJson(DocumentContext documentContext) {
        return new HocrInputJsonExt(documentContext.getDocument().getInputJson(), documentContext);
    }

    public static HocrInputJsonExt fromOcrTaskData(OcrTaskData ocrTaskData, HasStorage hasStorage) {
        HocrInputJsonExt result = empty(hasStorage);
        List images = new ArrayList();
        result.getJson().put(IMAGES, images);

        for (OcrResult ocrResult : ocrTaskData.getOcrResults()) {
            Map page = new HashMap();
            page.put(CONTENT, hasStorage.getStorageManager().getFileLink(hasStorage.getBucket(), ocrResult.getData().get(OcrFormats.IMAGE)));
            if (ocrResult.getData().get(OcrFormats.TA_INPUT) != null) {
                page.put(TESSINPUT, hasStorage.getStorageManager().getFileLink(hasStorage.getBucket(), ocrResult.getData().get(OcrFormats.TA_INPUT)));
            }

            page.put(JSON_SRC, hasStorage.getStorageManager().getFileLink(hasStorage.getBucket(), ocrResult.getData().get(OcrFormats.JSON)));
            page.put(HOCR_SRC, hasStorage.getStorageManager().getFileLink(hasStorage.getBucket(), ocrResult.getData().get(OcrFormats.HOCR)));
            if (ocrResult.getData().get(OcrFormats.TEXT) != null) {
                page.put(TEXT_SRC, hasStorage.getStorageManager().getFileLink(hasStorage.getBucket(), ocrResult.getData().get(OcrFormats.TEXT)));
            }

            Map dimensions = new HashMap();
            page.put(DIMENSIONS, dimensions);
            dimensions.put(WIDTH, ocrResult.getData().get(OcrResultKeys.IMAGE_WIDTH));
            dimensions.put(HEIGHT, ocrResult.getData().get(OcrResultKeys.IMAGE_HEIGHT));

            images.add(page);
        }
        return result;
    }

    public <D extends DpDocument> void migrateToDocumentContext(DocumentContext.DocumentExportContext<D> context) {

        for (HocrPage page : getPages()) {
            migrateS3PathToDocSet(page, JSON_SRC, context);
            migrateS3PathToDocSet(page, TESSINPUT, context);
            migrateS3PathToDocSet(page, CONTENT, context);
            migrateS3PathToDocSet(page, HOCR_SRC, context);
            migrateS3PathToDocSet(page, TEXT_SRC, context);
        }

    }

    private void migrateS3PathToDocSet(HocrPage page, String key, DocumentContext.DocumentExportContext context) {
        if (page.getJson().containsKey(key)) {
            page.getJson().put(key, getStorageManager().getFileLink(getBucket(), context.importS3PathIntoDocSet(page.resolveStoragePathByKey(key), true)));
        }
    }

    public MlTaskData prepareMlTaskData(DocumentContext documentContext, String... modelNameAndVersion) {
        if (!(modelNameAndVersion.length == 0 || modelNameAndVersion.length == 2)) {
            throw new IllegalArgumentException("Wrong model name and version specified for MlTaskData");
        }
        String modelName = modelNameAndVersion.length > 0 ? modelNameAndVersion[0] : documentContext.getRunModel().getName();
        String modelVersion = modelNameAndVersion.length > 0 ? modelNameAndVersion[1] : documentContext.getRunModel().getVersion();

        MlTaskData mlTaskData = new MlTaskData(modelName, modelVersion);

        List<Object> pages = new ArrayList<>();
        for (HocrPage result : getPages()) {
            Map<String, Object> page = new HashMap<>();
            page.put(PAGE, result.getPageNumber());
            Map<String, Object> data = new HashMap<>();
            page.put(DATA, data);
            data.put(OcrResultKeys.IMAGE_WIDTH, result.getWidth());
            data.put(OcrResultKeys.IMAGE_HEIGHT, result.getHeight());
            data.put(FORMAT, HOCR);
            data.put(OcrFormats.IMAGE, result.resolveStoragePathByKey(CONTENT));
            data.put(OcrFormats.HOCR, result.resolveStoragePathByKey(HOCR_SRC));
            data.put(OcrFormats.TEXT, result.resolveStoragePathByKey(TEXT_SRC));
            pages.add(page);
        }

        Map<String, Object> mlDocument = new HashMap<>();
        mlDocument.put(DOCUMENT_ID, documentContext.getDocumentId());
        mlDocument.put(PAGES, pages);
        mlTaskData.getData().put(DOCUMENTS, Arrays.asList(mlDocument));

        if (documentContext.getBucket() != null) {
            mlTaskData.getConfiguration().put(OcrConfiguration.Fields.bucket, documentContext.getBucket());
        }

        return mlTaskData;
    }

    /**
     * Saves all pages on storage.
     * WARNING: Use save method of the specific changed page for best performance instead.
     */
    @Override
    public void save() {
        for (HocrPage p : getPages()) {
            p.save();
        }
    }

    interface HasChildrenNode<N extends HocrPage.HocrNode> {

        List<N> getAllChild();

        Optional<N> findChild(List<Double> area);

    }

    @ToString(of = { "pageNumber" })
    public class HocrPage extends StorageContextJson implements HasChildrenNode<HocrPage.HocrArea>, Hocr2JsonProcessing {

        @Getter
        @Setter(AccessLevel.PACKAGE)
        private int pageNumber;

        @Getter(AccessLevel.PROTECTED)
        private final double[] baseBbox;

        public HocrPage(Map<String, Object> json, HasStorage hasStorage, int pageNumber) {
            super(json, hasStorage);
            this.pageNumber = pageNumber;
            this.baseBbox = new double[] { 0, 0, getWidth(), getHeight() };
        }

        public int getWidth() {
            return (int) Double.parseDouble(getKeyValue(DIMENSIONS + "." + WIDTH, "0"));
        }

        public int getHeight() {
            return (int) Double.parseDouble(getKeyValue(DIMENSIONS + "." + HEIGHT, "0"));
        }

        public String getContent() {
            return getKeyValue(CONTENT);
        }

        private String text = null;

        public String getText() {
            if (text == null) {
                text = resolveStorageRefContent(TEXT, "");
            }
            return text;
        }

        private Document document = null;

        private Document getDocument() {
            if (document == null) {
                document = Jsoup.parse(resolveStorageRefContent(HOCR, "<html></html>"));
            }
            return document;
        }

        private List<Double> bboxFromNode(Element src) {
            return Arrays.stream(resolveTitleBboxNormalized(src.attr(TITLE_ATTR), getBaseBbox())).mapToObj(d -> new Double(d)).collect(Collectors.toList());
        }

        private boolean isTheSame(Element e, List<Double> area) {
            return isTheSame(bboxFromNode(e), area);
        }

        private boolean isTheSame(List<Double> myArea, List<Double> area) {
            return doubleEquals(myArea.get(0), area.get(0)) && doubleEquals(myArea.get(1), area.get(1)) && doubleEquals(myArea.get(2), area.get(2)) && doubleEquals(
                    myArea.get(3), area.get(3));

        }

        public List<HocrPage.HocrWord> findWords(List<Double>... wordBboxes) {
            return findWords(Arrays.asList(wordBboxes));
        }

        public List<HocrPage.HocrWord> findWords(List<List<Double>> wordBboxes) {
            return getAllChild().stream().filter(a -> wordBboxes.stream().anyMatch(wordBbox -> a.isCovering(wordBbox))).map(a -> a.getAllChild()).flatMap(List::stream)
                    .filter(p -> wordBboxes.stream().anyMatch(wordBbox -> p.isCovering(wordBbox))).map(p -> p.getAllChild()).flatMap(List::stream)
                    .filter(l -> wordBboxes.stream().anyMatch(wordBbox -> l.isCovering(wordBbox))).map(l -> l.getAllChild()).flatMap(List::stream)
                    .filter(w -> wordBboxes.stream().anyMatch(wordBbox -> w.isCovering(wordBbox))).collect(Collectors.toList());

        }

        public List<HocrPage.HocrWord> getAllWords() {
            return getAllChild().stream().map(a -> a.getAllChild()).flatMap(List::stream).map(p -> p.getAllChild()).flatMap(List::stream).map(l -> l.getAllChild())
                    .flatMap(List::stream).collect(Collectors.toList());
        }

        public List<HocrPage.HocrLine> getAllLines() {
            return getAllChild().stream().map(a -> a.getAllChild()).flatMap(List::stream).map(p -> p.getAllChild()).flatMap(List::stream).collect(Collectors.toList());
        }

        public List<HocrPage.HocrParagraph> getAllParagraphs() {
            return getAllChild().stream().map(a -> a.getAllChild()).flatMap(List::stream).collect(Collectors.toList());
        }

        public List<HocrPage.HocrArea> getAllAreas() {
            return getAllChild();
        }

        public Optional<HocrPage.HocrWord> findWord(List<Double> wordBbox) {
            return findWords(wordBbox).stream().findFirst();
        }

        /**
         * Saves page on storage.
         */
        public void save() {
            updateStorageRefContent(HOCR, getDocument().html());
            updateStorageRefContent(JSON, JsonUtils.writeObjectToString(new Hocr2Json2JsonConverter().resolveDocumentNode(getDocument().html())));
        }

        @Override
        public List<HocrPage.HocrArea> getAllChild() {
            return getDocument().select(AREAS_SELECTOR).stream().map(e -> new HocrPage.HocrArea(this, e)).collect(Collectors.toList());
        }

        @Override
        public Optional<HocrPage.HocrArea> findChild(List<Double> area) {
            return getDocument().select(AREAS_SELECTOR).stream().filter(e -> HocrPage.this.isTheSame(e, area)).map(e -> new HocrPage.HocrArea(this, e)).findFirst();
        }

        abstract class HocrNode<M extends HocrPage.HocrNode, P extends HasChildrenNode<M>> implements Hocr2JsonProcessing {
            @Getter
            private P parent;

            @Getter(AccessLevel.PACKAGE)
            private Element src;

            @Getter
            private final String id;

            @Getter
            private final List<Double> bbox;

            public HocrNode(P parent, Element src) {
                this.parent = parent;
                this.src = src;
                this.id = src.attr(ID_ATTR);
                this.bbox = HocrPage.this.bboxFromNode(src);
            }

            /**
             * Returns true if the elements region is covered by the provided bbox.
             * @param area
             * @return
             */
            public boolean isCoveredBy(List<Double> area) {
                List<Double> myArea = getBbox();
                if (myArea == null || myArea.size() != 4 || area == null || area.size() != 4) {
                    return false;
                }
                return myArea.get(0) >= area.get(0) && myArea.get(1) >= area.get(1) && myArea.get(2) <= area.get(2) && myArea.get(3) <= area.get(3);
            }

            /**
             * Returns true if the elements region is covering the provided bbox.
             * @param area
             * @return
             */
            public boolean isCovering(List<Double> area) {
                List<Double> myArea = getBbox();
                if (myArea == null || myArea.size() != 4 || area == null || area.size() != 4) {
                    return false;
                }
                return myArea.get(0) <= area.get(0) && myArea.get(1) <= area.get(1) && myArea.get(2) >= area.get(2) && myArea.get(3) >= area.get(3);
            }

            public boolean isTheSame(List<Double> area) {
                return HocrPage.this.isTheSame(getBbox(), area);
            }

            public void remove() {
                src.remove();
            }

            @Override
            public String toString() {
                return this.getClass().getSimpleName() + "{ id=" + getId() + ", bbox=" + getBbox() + '}';
            }
        }

        abstract class OcrContainerNode<M extends HocrPage.HocrNode, P extends HasChildrenNode<M>, C extends HocrPage.HocrNode> extends HocrPage.HocrNode<M, P> implements HasChildrenNode<C> {

            public OcrContainerNode(P parent, Element src) {
                super(parent, src);
            }

            @Override
            public List<C> getAllChild() {
                return getSrc().select(childSelector()).stream().map(e -> newChild(e)).collect(Collectors.toList());
            }

            @Override
            public Optional<C> findChild(List<Double> area) {
                return getSrc().select(childSelector()).stream().filter(e -> HocrPage.this.isTheSame(e, area)).map(e -> newChild(e)).findFirst();
            }

            abstract C newChild(Element src);

            abstract String childSelector();

        }

        public class HocrArea extends HocrPage.OcrContainerNode<HocrPage.HocrArea, HocrPage, HocrPage.HocrParagraph> {
            public HocrArea(HocrPage parent, Element src) {
                super(parent, src);
            }

            @Override
            HocrPage.HocrParagraph newChild(Element src) {
                return new HocrPage.HocrParagraph(this, src);
            }

            @Override
            String childSelector() {
                return PARAGRAPHS_SELECTOR;
            }
        }

        public class HocrParagraph extends HocrPage.OcrContainerNode<HocrPage.HocrParagraph, HocrPage.HocrArea, HocrPage.HocrLine> {
            public HocrParagraph(HocrPage.HocrArea parent, Element src) {
                super(parent, src);
            }

            @Override
            HocrPage.HocrLine newChild(Element src) {
                return new HocrPage.HocrLine(this, src);
            }

            @Override
            String childSelector() {
                return LINES_SELECTOR;
            }
        }

        public class HocrLine extends HocrPage.OcrContainerNode<HocrPage.HocrLine, HocrPage.HocrParagraph, HocrPage.HocrWord> {

            public HocrLine(HocrPage.HocrParagraph parent, Element src) {
                super(parent, src);
            }

            @Override
            HocrPage.HocrWord newChild(Element src) {
                return new HocrPage.HocrWord(this, src);
            }

            @Override
            String childSelector() {
                return WORDS_SELECTOR;
            }
        }

        public class HocrWord extends HocrPage.HocrNode<HocrPage.HocrWord, HocrPage.HocrLine> {

            public HocrWord(HocrPage.HocrLine parent, Element src) {
                super(parent, src);
            }

            public String getText() {
                return getSrc().text();
            }

            public int getPage() {
                return getParent().getParent().getParent().getParent().getPageNumber();
            }

            /**
             * Confidence in percentage
             * @return
             */
            public double getConfidence() {
                return resolveTitleData(getSrc().attributes().get(TITLE_ATTR), X_WCONF);
            }

            public void setText(String text) {
                getSrc().text(text);
            }

        }
    }

    private List<HocrPage> pages = null;

    /**
     * Gets "pages"
     * @return list of pages
     */
    public synchronized List<HocrPage> getPages() {
        if (pages == null) {
            List<Map<String, Object>> images = (List<Map<String, Object>>) getJson().computeIfAbsent(IMAGES, k -> new ArrayList<>());
            pages = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                pages.add(new HocrPage(images.get(i), this, i));
            }
        }
        return pages;
    }

    private List<Map<String, Object>> getImages() {
        return (List<Map<String, Object>>) getJson().computeIfAbsent(IMAGES, k -> new ArrayList<>());
    }

    public synchronized Map<Integer, Integer> removePage(int index) {
        return removePages(new Integer(index));
    }

    public Map<Integer, Integer> removePages(Integer... pages) {
        return removePages(Arrays.asList(pages).stream().collect(Collectors.toSet()));
    }

    public Map<Integer, Integer> removePages(Set<Integer> pageIndexes) {
        return removePages(pageIndex -> pageIndexes.contains(pageIndex));
    }

    public Map<Integer, Integer> leaveOnlyPages(Set<Integer> pageIndexes) {
        return removePages(pageIndex -> !pageIndexes.contains(pageIndex));
    }

    private Map<Integer, Integer> removePages(Predicate<Integer> pageTester) {
        Map<Integer, Integer> pagesMapping = new HashMap<>();
        pages = getPages().stream().filter(p -> !pageTester.test(p.getPageNumber())).collect(Collectors.toList());
        for (int i = 0; i < pages.size(); i++) {
            pagesMapping.put(pages.get(i).getPageNumber(), i);
            pages.get(i).setPageNumber(i);
        }
        getJson().put(IMAGES, pages.stream().map(p -> p.getJson()).collect(Collectors.toList()));
        return pagesMapping;
    }

    public synchronized Optional<HocrPage> getPage(int index) {
        if (getPages().size() >= index) {
            return Optional.of(pages.get(index));
        } else {
            return Optional.empty();
        }
    }

}