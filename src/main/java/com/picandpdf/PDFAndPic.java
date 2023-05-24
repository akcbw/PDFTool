package com.picandpdf;


import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class PDFAndPic {

    private static final List IMAGE_SUFFIX = Arrays.asList("jpg", "png", "jpeg");

    public static void pdf2png(String fileAddress, String filename, String type) {
        long startTime = System.currentTimeMillis();
        // 将文件地址和文件名拼接成路径 注意：线上环境不能使用\\拼接
        File file = new File(fileAddress + "\\" + filename);
        try {
            // 写入文件
            PDDocument doc = PDDocument.load(file);
            PDFRenderer renderer = new PDFRenderer(doc);
            int pageCount = doc.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                // dpi为144，越高越清晰，转换越慢
                BufferedImage image = renderer.renderImageWithDPI(i, 144); // Windows native DPI
                // 将图片写出到该路径下
                ImageIO.write(image, type, new File(fileAddress + "\\" + filename + "_" + (i + 1) + "." + type));
            }
            long endTime = System.currentTimeMillis();
            System.out.println("共耗时：" + ((endTime - startTime) / 1000.0) + "秒");  //转化用时
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void imagesToPdf(String imageFolder, String fileName) throws IOException {
        File tempImage;
        String tempImageName;
        int index;
        PDDocument outDocument = new PDDocument();

        File imageFiles = new File(imageFolder);

        for (int i = 0; i < imageFiles.listFiles().length; i++) {
            tempImage = imageFiles.listFiles()[i];
            if (!tempImage.isFile()) {
                continue;
            }

            tempImageName = tempImage.getName();
            index = tempImageName.lastIndexOf(".");
            if (index == -1) {
                continue;
            }
            //获取文件的后缀
            String suffix = tempImageName.substring(index + 1);
            //如果文件后缀不是图片格式,跳过当前循环
            if (!IMAGE_SUFFIX.contains(suffix)) {
                continue;
            }

            BufferedImage image = ImageIO.read(tempImage);
            PDImageXObject imageXObject = LosslessFactory.createFromImage(outDocument, image);

            PDPage pdPage = new PDPage(PDRectangle.A4);
            outDocument.addPage(pdPage);

            PDPageContentStream pageContentStream = new PDPageContentStream(outDocument, pdPage);

            float height = pdPage.getMediaBox().getHeight();//要将图片在pdf中绘制多高，这里宽度直接使用了pdfpage的宽度，即横向铺满，这里的height也是使用了pdfpage的高度。因此最终结果是铺满整个pdf页。
            float width = pdPage.getMediaBox().getWidth();

            float imageHeight = imageXObject.getHeight();
            float imageWidth = imageXObject.getWidth();


            if (imageHeight > imageWidth) {
                float v = imageHeight / height;
                float h = imageWidth / width;

                if (v > h) {
                    float actWidth = width * (imageHeight / imageWidth);
                    pageContentStream.drawImage(imageXObject, (width - actWidth) /2, 0, actWidth, height);
                } else {
                    float actHeight = height * (imageWidth / imageHeight);
                    pageContentStream.drawImage(imageXObject, 0, (height - actHeight)/2, width, actHeight);
                }
            } else {
                float actHeight = width * (imageHeight / imageWidth);
                pageContentStream.drawImage(imageXObject, 0, (height - actHeight) / 2, width, actHeight);
            }

            pageContentStream.setHorizontalScaling(200);
            pageContentStream.close();
        }

        outDocument.save(imageFolder + "\\" + fileName);
        outDocument.close();
    }

    public static void splitPdfs(String pdfFolder, String pdfName) throws IOException {
        File file = new File(pdfFolder + "\\" + pdfName);
        PDDocument document = PDDocument.load(file);
        //Instantiating Splitter class
        Splitter splitter = new Splitter();


        splitter.setStartPage(1);
        splitter.setSplitAtPage(1);
        splitter.setEndPage(document.getNumberOfPages());

        //splitting the pages of a PDF document
        List<PDDocument> Pages = splitter.split(document);

        // Creating an iterator
        Iterator<PDDocument> iterator = Pages.listIterator();
        // Saving each page as an individual document

        int j = 1;
        pdfName = pdfName.replace(".pdf", "");
        while(iterator.hasNext()) {
            PDDocument pd = iterator.next();
            String pdfName2 = pdfFolder + "/" + pdfName+ "_"+ j++ +".pdf";
            pd.save(pdfName2);
            pd.close();
        }
        document.close();
    }

    public static void mergePdfs(String pdfFolder, String mergedName) throws IOException {
        File folder = new File(pdfFolder);
        File[] pdfFiles = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".pdf");
            }
        });

        List<InputStream> inputStreams = new ArrayList<>();
        for(File pdfFile:pdfFiles) {
            inputStreams.add(new FileInputStream(pdfFile));
        }

        PDFMergerUtility mergePdf = new PDFMergerUtility();
        mergePdf.addSources(inputStreams);
        mergePdf.setDestinationFileName(pdfFolder + "\\" + mergedName);
        mergePdf.mergeDocuments();

        for (InputStream in : inputStreams) {
            in.close();
        }
    }


    public static void main(String[] args) throws IOException {
        //PNGToPDF
//        String imageFolder = "D:\\bowen";
//        String fileName = "test2.pdf";
//        imagesToPdf(imageFolder, fileName);



        //PDF to PNG
//        String fileAddres = "D:\\bowen";
//        String fileName = "ISTQB软件测试专业术语对照表.pdf";
//        pdf2png(fileAddres, fileName, "png");


        //merge pdf
        String pdfFolder = "D:\\bowen\\test_merge";
        String mergedName = "mergePdf.pdf";
        mergePdfs(pdfFolder, mergedName);

        //split pdf
//        String pdfFolder = "D:\\bowen";
//        String fileName = "test2.pdf";
//        splitPdfs(pdfFolder, fileName);
    }
}
