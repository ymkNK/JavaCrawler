package imageDownload;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

import com.oracle.tools.packager.Log;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BreadthCrawler;
import cn.edu.hfut.dmic.webcollector.util.Config;
import cn.edu.hfut.dmic.webcollector.util.FileUtils;

public class DemoImageCrawler extends BreadthCrawler {
    // 下载路径
    File downloadDir;
    // 用于生成图片名称的数字
    AtomicInteger imageId;

    /**
     * 爬行图片
     *
     * @param crawlPath    爬行的路径
     * @param downloadPath 解析
     */
    public DemoImageCrawler(String crawlPath, String downloadPath) {

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
        System.out.println(page.url());
        // 根据http来判断当前的资源是图片还是html
        String contentType = page.contentType();
        // 在判断当前的资源是否为html
        LOG.info("contentType:{}", contentType);
        if (contentType == null) {
            return;
        } else if (contentType.contains("html")) {
            //如果有colorbox_gallery类遍历之
            Elements imglinks = page.select("a.colorbox_gallery");
            for (Element imglink : imglinks) {
                String attr1 = imglink.attr("abs:colorbox_gallery");
                next.add(attr1);
            }
            // 如果有图片的,就拿图片那部分保存在文件中
            Elements imgs = page.select("img[src]");
            // 然后再遍历所有html中所有图片
            for (Element img : imgs) {
                String attr = img.attr("abs:src");
                next.add(attr);
            }
        }
        // 在判断如果是起始图片就直接下载
        else if ((contentType.startsWith("image")) || (Pattern.matches(".*jpg$", page.url()))) {
            //System.out.println("已找到图片！");
            //判断图片大小，只抓取高清图片
            ByteArrayInputStream bais = new ByteArrayInputStream(page.content());
            try {
                BufferedImage img = ImageIO.read(bais);
                if ((img.getHeight() > 100) && (img.getWidth() > 100)) {
                    System.out.println("已找到高清图片！");
                    //进行切割加成名称
                    String extensionName = contentType.split("/")[1];
                    String imageFileName = imageId.incrementAndGet() + "." + extensionName;
                    File imageFile = new File(downloadDir, imageFileName);
                    try {
                        FileUtils.write(imageFile, page.content());
                        System.out.println("保存图片" + page.url() + "到" + imageFile.getAbsolutePath());
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                LOG.error("error,{}", e);
            } finally {
                try {
                    bais.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    LOG.error("error,{}", e);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DemoImageCrawler demoImageCrawler = new DemoImageCrawler("crawlPath", "downloadPath");
        // 添加url
        // 添加爬取范围
    /*start page*/
        demoImageCrawler.addSeed("https://www.gooood.cn/le-cheng-kindergarten-china-by-mad.htm");
        //demoImageCrawler.addRegex("https://oss.gooood.cn/uploads/*.*");
    /*fetch url like http://news.hfut.edu.cn/show-xxxxxxhtml*/
        //demoImageCrawler.addSeed("https://oss.gooood.cn/uploads");
        //demoImageCrawler.addRegex("-.*/");
        // 设置每次爬取都重新开始
        demoImageCrawler.setResumable(false);
        // 每次开启了30个线程
        demoImageCrawler.setThreads(30);
        // 大小
        //Config.MAX_RECEIVE_SIZE = 1000 * 1000 * 10;
        // 开发
        demoImageCrawler.start(50);
        System.out.println(demoImageCrawler);
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