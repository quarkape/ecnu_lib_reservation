import com.sun.mail.util.MailSSLSocketFactory;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

public class SendEmailUtil {

    // 邮件服务器主机名
    // QQ邮箱的 SMTP 服务器地址为: smtp.qq.com
    //发件人邮箱
    private static String myEmailAccount = "xxx@qq.com";
    // 发件人邮箱密码授权码，如何获取授权码请参考：https://service.mail.qq.com/detail/0/75
    private static String myEmailPassword = "xxx";
    // 接收预约结果通知的邮箱，建议使用qq邮箱，然后在微信上绑定qq邮箱，这样每次预约之后能够在微信上收到预约结果
    private static final String toEmailAddress = "xxx@qq.com";
    // 邮件标题
    private static final String emailSubject = "华东师范大学图书馆座位预约结果";
    // 发送邮件
    public static boolean sendEmail(String content) throws Exception {

        Properties props = new Properties();

        // debug调试
        props.setProperty("mail.debug", "false");

        // 发送服务器需要身份验证
        props.setProperty("mail.smtp.auth", "true");

        // 端口号
        props.put("mail.smtp.port", 465);

        // 设置邮件服务器主机名
        props.setProperty("mail.smtp.host", "smtp.qq.com");

        // 发送邮件协议名称
        props.setProperty("mail.transport.protocol", "smtp");

        // SSL认证，注意腾讯邮箱是基于SSL加密的，所以需要开启才可以使用
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);

        // 设置是否使用ssl安全连接（一般都使用）
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.socketFactory", sf);

        // 创建会话
        Session session = Session.getInstance(props);

        // 发送的消息，基于观察者模式进行设计的
        Message msg = new MimeMessage(session);

        // 设置邮件标题
        msg.setSubject(emailSubject);

        // 设置邮件内容，使用StringBuilder，因为StringBuilder加载速度会比String快，而且线程安全性也不错
        StringBuilder builder = new StringBuilder();

        // 写入内容
        builder.append(content);

        // 设置显示的发件时间
        msg.setSentDate(new Date());

        // 设置邮件内容
        msg.setContent(builder.toString(), "text/html;charset=UTF-8");

        // 设置发件人邮箱
        msg.setFrom(new InternetAddress(myEmailAccount, "自己", "UTF-8"));

        // 得到邮差对象
        Transport transport = session.getTransport();

        // 连接自己的邮箱账户
        transport.connect("smtp.qq.com", myEmailAccount, myEmailPassword);

        // 发送邮件
        transport.sendMessage(msg, new Address[]{new InternetAddress(toEmailAddress)});

        // 关闭连接
        transport.close();

        return true;
    }

}
