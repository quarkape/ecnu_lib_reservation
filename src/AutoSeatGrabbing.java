import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import com.alibaba.fastjson2.JSONObject;
import com.sun.mail.util.MailSSLSocketFactory;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class AutoSeatGrabbing {

    private static final String BASE_URL = "https://libseat.ecnu.edu.cn/api.php";
    private static final String SKALIB_URL = "http://www.skalibrary.com";
    private static final String SCHOOL = "ecnu";
    private static final String SCHOOL_NAME = "华东师范大学";
    private static final String OPENID = ""; // openId，固定，用于绑定用户
    private static final String PASSWORD = ""; // 用户密码
    private static final String USERNAME = ""; // 学号
    private static final String EMAIL = "xxx@qq.com"; // 接收预约结果通知的邮箱，建议使用qq邮箱，然后在微信上绑定qq邮箱，这样每次预约之后能够在微信上收到预约结果
    // 邮件服务器主机名 QQ邮箱的 SMTP 服务器地址为: smtp.qq.com
    private static String myEmailSMTPHost = "smtp.qq.com";
    //发件人邮箱
    private static String myEmailAccount = "xx@qq.com";

    // 发件人邮箱密码 授权码 在开启SMTP服务时会获取到一个授权码，把授权码填在这里
    private static String myEmailPassword = "cspyhaclxvcadife";
    private static HttpURLConnection con = null;
    private static BufferedReader bufferedReader = null;
    private static InputStream inputStream = null;
    private static StringBuffer stringBuffer = null;
    private static SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");
    private static final String TYPE = "1";
    private static final String SEAT_ID = "xxxx"; // 默认座位id，可以修改
    private static final String[] SEATS_ID = {"xxxx"}; // 默认座位id数组，可以修改

    /**
     * 主函数，使用crontab设置定时任务的时候，会默认执行这个函数
     * 这里面两个语句，选其中一个执行就可以了
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // 搭配crontab使用，在预约请求之前先获取token和segment参数，存入token.txt当中
        autoGrabSeat(SEAT_ID);


        // 预约SEAT_ID单个座位，如果失败，不会继续尝试
//        autoGrabSeat(SEAT_ID);

        // 尝试SEATS_ID数组中的所有座位，直到某个座位预约成功或者全部预约失败
        // bookOneSeat();
    }

    /**
     * 预约座位，包括登录-->获取segment参数-->开始预约三个主要流程
     * @throws Exception
     */
    public static boolean autoGrabSeat(String seatId) throws Exception {
        // 获取明天日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        // 下面这行代码最后日期增加1，意思是今天预约明天的座位
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        String tomorrow = formatter.format(calendar.getTime());
        String content = "<div>位置id：" + seatId + "</div><div>预约日期：" + tomorrow + "</div>";

        /**
         * 解除当前用户绑定
         * 如果已经在公众号上面绑定了，可以不用调用removeUser()
         */
//        Boolean hasLogOut = removeUser();
        /**
         * 登录以获取access_token
         * 如果没有预先绑定的话，会登录失败，这时候可以先去公众号上面绑定一下
         */
        HashMap<String, String> map = login();
        if (map.get("status") == "0") {
            content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：初始登录失败</div>";
            sendEmail(EMAIL, content);
            return false;
        }
        /**
         * 绑定用户
         * 如果已经在公众号上面绑定过了，就不需要执行下面的addUser()
         */
//        Boolean addUser = addUser(map.get("name"), map.get("card"), map.get("deptName"), map.get("gender"), map.get("roleName"));
//        if (!addUser) {
//            content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：绑定失败，请检查学号或密码</div>";
//            SendEmailUtil.sendEmail(EMAIL, content);
//            return false;
//        }
        /**
         * 通过getViableTime()获取segement参数
         */
        HashMap<String, String> map1 = getViableTime("40", tomorrow);
        if (map1.get("status") == "0") {
            content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：获取segement参数的过程中失败</div>";
            sendEmail(EMAIL, content);
            return false;
        }
        /**
         * 开始抢座
         */
        HashMap<String, String> res = grabSeat(map.get("accessToken"), TYPE, map1.get("segment"), seatId);
        if (res.get("status") == "1") {
            sendEmail(EMAIL, "<div>预约结果：预约成功！</div>" + content + "<div><b>莫等闲，白了少年头，空悲切！</b></div>");
        } else {
            content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：" + res.get("msg") + "</div>";
            sendEmail(EMAIL, content);
            return false;
        }
        return true;
    }

    /**
     * 预先获取一个token，时间到了直接开始执行请求
     * @throws Exception
     */
    public static boolean preTokenBook(String seatId) throws Exception {// 获取明天日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        // 下面这行代码最后日期增加1，意思是今天预约明天的座位
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        String tomorrow = formatter.format(calendar.getTime());
        String content = "<p></p><div>位置id：" + seatId + "</div><p></p><div>预约日期：" + tomorrow + "</div>";
        String paramsStr = readFile();
        if (paramsStr.length() == 0) {
            writeFile("");
            HashMap<String, String> map = login();
            if (map.get("status") == "0") {
                content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：初始登录失败</div>";
                sendEmail(EMAIL, content);
                return false;
            }
            HashMap<String, String> map1 = getViableTime("40", tomorrow);
            if (map1.get("status") == "0") {
                content = "<div>预约结果：预约失败</div>" + content + "<div>失败原因：获取segement参数的过程中失败</div>";
                sendEmail(EMAIL, content);
                return false;
            }
            // 将token存入本地文件中
            writeFile(map.get("accessToken") + ":" + map1.get("segment"));
//            sendEmail(myEmailAccount, "accessToken获取成功，获取时间：" + formatter.format(new Date()));
            return true;
        }
        String accessToken = paramsStr.split(":")[0];
        String segment = paramsStr.split(":")[1];
        HashMap<String, String> res = grabSeat(accessToken, TYPE, segment, seatId);
        if (res.get("status") == "1") {
            sendEmail(EMAIL, "<div>预约结果：预约成功！</div>" + content + "<p></p><div><b>莫等闲，白了少年头，空悲切！</b></div>");
        } else {
            sendEmail(EMAIL, "<div>预约结果：预约失败！</div>" + content + "<div>失败原因原始：" + res.get("msg") + "</div>");
        }
        // 在退出程序之前把本地文件中的内容清除掉，便于下次使用
        writeFile("");
        return true;
    }

    /**
     * 用户登录
     * @return
     * @throws IOException
     */
    public static HashMap<String, String> login() throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "0");
        JSONObject obj = requestPost(BASE_URL + "/login", "from=mobile&password=" + PASSWORD + "&username=" + USERNAME);
        if (obj.getInteger("status") == 1) {
            JSONObject infoObj = obj.getJSONObject("data").getJSONObject("list");
            map.replace("status", "1");
            map.put("name", infoObj.getString("name"));
            map.put("card", infoObj.getString("card"));
            map.put("deptName", infoObj.getString("deptName"));
            map.put("gender", infoObj.getString("gender"));
            map.put("roleName", infoObj.getString("roleName"));
            map.put("accessToken", obj.getJSONObject("data").getJSONObject("_hash_").getString("access_token"));
        }
        return map;
    }

    /**
     * skalibrary绑定用户
     * @param name 姓名
     * @param card 卡号
     * @param deptName
     * @param gender
     * @param roleName
     * @return
     * @throws IOException
     */
    public static Boolean addUser(String name, String card, String deptName, String gender, String roleName) throws IOException {
        String paramsStr = "openid=" + OPENID + "&username=" + USERNAME + "&password=" + PASSWORD + "&name=" + name + "&card=" + card + "&deptName=" + deptName + "&gender=" + gender + "&roleName=" + roleName + "&school=" + SCHOOL + "&schoolName=" + SCHOOL_NAME;
        JSONObject obj = requestPost(SKALIB_URL + "/addUser", paramsStr);
        return obj.getBoolean("status");
    }

    /**
     * skalibrary解除绑定
     * @return
     * @throws IOException
     */
    public static Boolean removeUser() throws IOException {
        JSONObject obj = requestPost(SKALIB_URL + "/removeUser", "openid=" + OPENID);
        return obj.getBoolean("status");
    }

    /**
     * 预约
     * @param accessToken
     * @param type
     * @param segment
     * @param seatId 座位id,如0001
     * @return
     * @throws IOException
     */
    public static HashMap<String, String> grabSeat(String accessToken, String type, String segment, String seatId) throws IOException {
        String paramStr = "access_token=" + accessToken + "&userid=" + USERNAME + "&type=" + type + "&id=" + seatId + "&segment=" + segment;
        JSONObject obj = requestPost(BASE_URL + "/spaces/" + seatId + "/book", paramStr);
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "0");
        if (obj.getInteger("status") == 1 && obj.getString("msg").indexOf("预约成功") != -1) {
            map.replace("status", "1");
        }
        map.put("msg", obj.getString("msg"));
        return map;
    }

    /**
     * 获取图书馆区域信息
     * @return
     * @throws IOException
     */
    public static String getAreaInfo() throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/areas?tree=1");
        if (obj.getInteger("status") == 0) {
            String areaId = obj.getJSONObject("data").getJSONArray("list").getJSONObject(0).getJSONArray("_child").getJSONObject(0).getJSONArray("_child").getJSONObject(4).getString("id");
            return areaId;
        }
        return "error";
    }

    /**
     * 获取可预约日期
     * @param areaCode 图书馆区域代码(从getAreaInfo()中获取) : 40 : 中北一楼B区
     * @throws IOException
     */
    public static void getViableDate(String areaCode) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/space_days/" + areaCode);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    /**
     * 获取可预约时间段
     * @param areaCode
     * @param date 如2023-03-05
     * @return
     * @throws IOException
     */
    public static HashMap<String, String> getViableTime(String areaCode, String date) throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "0");
        JSONObject obj = requestGet(BASE_URL + "/space_time_buckets?area=" + areaCode + "&day=" + date);
        if (obj.getInteger("status") == 1) {
            map.replace("status", "1");
            String segment = obj.getJSONObject("data").getJSONArray("list").getJSONObject(0).getString("id");
            String spaceId = obj.getJSONObject("data").getJSONArray("list").getJSONObject(0).getString("spaceId");
            map.put("segment", segment);
            map.put("spaceId", spaceId);
        }
        return map;
    }

    /**
     * 获取座位预约信息
     * @param areaCode
     * @param date
     * @param endTime 结束时间，华师大是23:50
     * @param segment 亲测这个参数不是必须的，可以不用，因为要得到这个参数还需要发起一次其他的请求
     * @param startTime
     * @throws IOException
     */
    public static void getSeatInfo(String areaCode, String date, String endTime, String segment, String startTime) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/spaces_old?area=" + areaCode + "&day=" + date + "&endTime=" + endTime + "&segment=" + segment + "&startTime=" + startTime);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    /**
     * 获取空间信息
     * @param spaceId 空间id,从getViableTime()中获取
     * @throws IOException
     */
    public static void getAreaInfo(String spaceId) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/spaces/" + spaceId);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    /**
     * 获取预约历史
     * @param accessToken
     * @param userid
     * @throws IOException
     */
    public static void getBookHistory(String accessToken, String userid) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/profile/books?access_token=" + accessToken + "&userid=" + userid);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    /**
     * 对多个座位进行检查，如果某个座位预约失败，则自动预约下一个座位
     * @return
     * @throws Exception
     */
    public static Boolean bookOneSeat() throws Exception {
        for (int i=0;i<SEATS_ID.length;i++) {
            if (!autoGrabSeat(SEATS_ID[i])) {
                continue;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * 请求配置
     * @param oriUrl
     * @param method
     * @param paramsStr
     * @return
     * @throws IOException
     */
    public static JSONObject requestConfig(String oriUrl, String method, String paramsStr) throws IOException {
        URL url = new URL(oriUrl);
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; OXF-AN10 Build/HUAWEIOXF-AN10; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/107.0.5304.141 Mobile Safari/537.36 XWEB/5015 MMWEBSDK/20221206 MMWEBID/838 MicroMessenger/8.0.32.2300(0x2800205D) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64");
        con.setRequestProperty("X-Forwarded-For", "219.228.146.228");
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setUseCaches(false);
        if (method.toUpperCase() == "POST") {
            OutputStream outputStream = con.getOutputStream();
            outputStream.write(paramsStr.getBytes());
        }
        int resCode = con.getResponseCode();
        JSONObject res;
        if (resCode == HttpURLConnection.HTTP_OK) {
            inputStream = con.getInputStream();
            stringBuffer = new StringBuffer();
            String line;
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }
//            System.out.println("请求结果:" + stringBuffer.toString());
            res = JSONObject.parseObject(stringBuffer.toString());
        } else {
//            inputStream = con.getErrorStream();
//            stringBuffer = new StringBuffer();
//            String line;
//            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
//            while ((line = bufferedReader.readLine()) != null) {
//                stringBuffer.append(line);
//            }
            res = new JSONObject();
            res.put("status", 0);
        }
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    /**
     * GET请求
     * @param oriUrl
     * @return
     * @throws IOException
     */
    public static JSONObject requestGet(String oriUrl) throws IOException {
        return requestConfig(oriUrl, "GET", null);
    }

    /**
     * POST请求
     * @param oriUrl
     * @param paramsStr
     * @return
     * @throws IOException
     */
    public static JSONObject requestPost(String oriUrl, String paramsStr) throws IOException {
        return requestConfig(oriUrl, "POST", paramsStr);
    }

    // 发送邮件
    public static void sendEmail(String toEmailAddress, String content) throws Exception {

        Properties props = new Properties();

        // 开启debug调试
        props.setProperty("mail.debug", "false");

        // 发送服务器需要身份验证
        props.setProperty("mail.smtp.auth", "true");

        // 端口号
        props.put("mail.smtp.port", 465);

        // 设置邮件服务器主机名
        props.setProperty("mail.smtp.host", myEmailSMTPHost);

        // 发送邮件协议名称
        props.setProperty("mail.transport.protocol", "smtp");

        /**SSL认证，注意腾讯邮箱是基于SSL加密的，所以需要开启才可以使用**/
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);

        //设置是否使用ssl安全连接（一般都使用）
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.socketFactory", sf);

        //创建会话
        Session session = Session.getInstance(props);

        //获取邮件对象
        //发送的消息，基于观察者模式进行设计的
        Message msg = new MimeMessage(session);

        //设置邮件标题
        msg.setSubject("华东师范大学图书馆自动预约结果");

        //设置邮件内容
        //使用StringBuilder，因为StringBuilder加载速度会比String快，而且线程安全性也不错
        StringBuilder builder = new StringBuilder();

        //写入内容
        builder.append(content);

        //设置显示的发件时间
        msg.setSentDate(new Date());

        //设置邮件内容
//        msg.setText(builder.toString());
        msg.setContent(builder.toString(), "text/html;charset=UTF-8");
        //设置发件人邮箱
        // InternetAddress 的三个参数分别为: 发件人邮箱, 显示的昵称(只用于显示, 没有特别的要求), 昵称的字符集编码
        msg.setFrom(new InternetAddress(myEmailAccount, "自己", "UTF-8"));

        //得到邮差对象
        Transport transport = session.getTransport();

        //连接自己的邮箱账户
        //密码不是自己QQ邮箱的密码，而是在开启SMTP服务时所获取到的授权码
        //connect(host, user, password)
        transport.connect(myEmailSMTPHost, myEmailAccount, myEmailPassword);

        //发送邮件
        transport.sendMessage(msg, new Address[]{new InternetAddress(toEmailAddress)});

        //将该邮件保存到本地
//        OutputStream out = new FileOutputStream("MyEmail.eml");
//        msg.writeTo(out);
//        out.flush();
//        out.close();

        transport.close();
    }

    /**
     * 读取外部文件
     */
    public static String readFile() {
        String pathname = "token.txt";
        String paramsStr = "";
        try (FileReader reader = new FileReader(pathname);
             BufferedReader br = new BufferedReader(reader)
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                paramsStr += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paramsStr;
    }

    /**
     * 写入外部文件
     */
    public static void writeFile(String s) {
        try {
            File writeName = new File("token.txt");
            writeName.createNewFile();
            try (FileWriter writer = new FileWriter(writeName);
                 BufferedWriter out = new BufferedWriter(writer)
            ) {
                out.write(s);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
