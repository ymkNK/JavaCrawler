package imageDownload;

import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BreadthCrawler;
import cn.edu.hfut.dmic.webcollector.util.FileUtils;
import com.google.gson.JsonObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Created by ymk on 2019/3/14.
 */
public class ArchDailyImageCrawler extends BreadthCrawler {
    // 下载路径
    File downloadDir;
    // 用于生成图片名称的数字
    AtomicInteger imageId;

    /**
     * @param crawlPath    爬取的路径?
     * @param downloadPath 要保存下来的爬虫的地址
     * @return
     * @author YangMingkai
     * @description //TODO 爬虫构造函数
     * @date 14:31 2019/3/14
     **/
    public ArchDailyImageCrawler(String crawlPath, String downloadPath) {

        super(crawlPath, true);
        // 创建一个文件
        downloadDir = new File(downloadPath);
        // 判断是否存在,如果不存在就,执行mkdirs方法
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        computeImageId();
    }

    /**
     * 访问 Page 路径 CrawlDatums 爬虫数据
     */

    @Override

    public void visit(Page page, CrawlDatums next) {
        String contentType = page.contentType();
        LOG.info("当前页面url:{}", page.url());
        // 根据http来判断当前的资源是图片还是html
        LOG.info("contentType:{}", contentType);

        /**
         * 在判断当前的资源是否为html
         * 主要的种类为以下三种
         * contentType:image/jpeg
         * contentType:image/png
         * contentType:text/html; charset=utf-8
         **/

        if (contentType == null) {
            return;
        } else if (contentType.contains("html")) {
            //如果有colorbox_gallery类遍历之
            Elements imglinks = page.select("a.colorbox_gallery");
            for (Element imglink : imglinks) {
                String attr1 = imglink.attr("abs:colorbox_gallery");
                LOG.info("colorbox_gallery找到图片,链接为{}",attr1);
                next.add(attr1);
            }
            // 如果有图片的,就拿图片那部分保存在文件中
            Elements imgs = page.select("img[src]");
            // 然后再遍历所有html中所有图片
            for (Element img : imgs) {
                String attr = img.attr("abs:src");
                String attrRaw=attr;
                attrRaw=attrRaw.replace("-150x150.jpg",".jpg");
                LOG.info("img找到图片,链接为{}",attr);
                LOG.info("对应原图attrRaw,链接为{}",attrRaw);
                next.add(attr);
                next.add(attrRaw);
            }


        }
        // 在判断如果是起始图片就直接下载
        else if ((contentType.startsWith("image")) || (Pattern.matches(".*png$", page.url())) || (Pattern.matches(".*jpg$", page.url()))) {
            //System.out.println("已找到图片！");
            //判断图片大小，只抓取高清图片
            downLoadImage(page, contentType);
        }
    }


    /**
     * @author YangMingkai
     * @description //TODO 将图片进行保存
     * @date 15:21 2019/3/14
     * @param page
     * @param contentType
     * @return
     **/
    private void downLoadImage(Page page, String contentType) {
        ByteArrayInputStream bais = new ByteArrayInputStream(page.content());
        try {
            BufferedImage img = ImageIO.read(bais);
            if ((img.getHeight() > 100) && (img.getWidth() > 100)) {
                LOG.info("已找到高清图片！");
                //进行切割加成名称
                String extensionName = contentType.split("/")[1];
                String imageFileName = imageId.incrementAndGet() + "." + extensionName;
                File imageFile = new File(downloadDir, imageFileName);
                try {
                    FileUtils.write(imageFile, page.content());
                    LOG.info("保存图片{}到{}", page.url(), imageFile.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    LOG.error("file not found :{}", e);
                } catch (IOException e) {
                    LOG.error("IO Exception:{}", e);
                }
            }
        } catch (IOException e) {
            LOG.error("error,{}", e);
        } finally {
            try {
                bais.close();
            } catch (IOException e) {
                LOG.error("error,{}", e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ArchDailyImageCrawler archDailyImageCrawler = new ArchDailyImageCrawler("crawlPath", "downloadPath");
        archDailyImageCrawler.addSeed("https://www.gooood.cn/le-cheng-kindergarten-china-by-mad.htm");
        archDailyImageCrawler.setResumable(false);
        archDailyImageCrawler.setThreads(30);
        archDailyImageCrawler.start(50);
        System.out.println(archDailyImageCrawler);
    }

    /**
     * 计算图片的id
     */

    public void computeImageId() {
        int maxId = 1;
        // 遍历出来所有图片文件
        for (File imageFile : downloadDir.listFiles()) {
            // 获取图片的名称
            String fileName = imageFile.getName();
            // 获取到名字,xxx\xxx.jsp,进行切割
            String idStr = fileName.split("\\.")[0];
            // 获取数字来做名称
            Integer id = Integer.valueOf(maxId);
            if (id > maxId) {
                id = maxId;
            }
        }
        // 创建一个新的原来数字
        imageId = new AtomicInteger(maxId);
    }


}