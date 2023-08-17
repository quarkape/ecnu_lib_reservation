import java.awt.geom.Area;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.alibaba.fastjson2.JSONObject;

/**
 * author: wanghao
 * create_time: 2023-8-17
 * description: 使用前，请在华东师范大学图书馆公众号上面绑定用户（以华师大使用者为例）
 * usage：请参看 README.md 查看详细使用方法
 */

public class SeatReservation {

    // 华东师范大学作为预约接口 API
    // 请通过抓包获取你所在高校的 API 地址
    private static final String BASE_URL = "https://libseat.ecnu.edu.cn/api.php";

    // 学号和密码，用于每次预约登录
    private static final String USERNAME = "xxxx";
    private static final String PASSWORD = "xxxx";

    private static HttpURLConnection con = null;
    private static BufferedReader bufferedReader = null;
    private static InputStream inputStream = null;
    private static StringBuffer stringBuffer = null;

    private static SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");

    private static final String TYPE = "1";
    // 默认座位id
    // 参考 README.md 了解如何获取
    private static final String SEAT_ID = "xxxx";
    // 默认座位 id 数组，用于对一组座位进行预约
    private static final String[] SEATS_ID = {"xxxx", "xxxx", "xxxx"};
    // 区域代号
    // 参考 README.md 了解如何获取
    private static final String AreaCode = "xxxx";
    // 是否开启邮件通知
    private static final boolean emailInfo = false;

    /**
     * 主函数，使用crontab设置定时任务的时候，会默认执行这个函数
     * 提供两种预约方式，根据需要自行配置和选择
     */
    public static void main(String[] args) throws Exception {

        // 预约SEAT_ID单个座位，如果失败，不会继续尝试
        beginSeatReservation(SEAT_ID);

        // 尝试SEATS_ID数组中的所有座位，直到某个座位预约成功或者全部预约失败
        // bookSeatLoop();
    }

    /**
     * 预约座位
     * 包括登录 --> 获取segment参数 --> 开始预约三个主要流程
     */
    public static boolean beginSeatReservation(String seatId) throws Exception {
        // 获取明天日期
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        String tomorrow = formatter.format(calendar.getTime());

        // 登录以获取access_token，如果没有预先绑定的话，会登录失败，需要先去公众号上面绑定一下
        HashMap<String, String> map = login();
        System.out.println("0");
        if (map.get("status") == "0") {
            return emailInfo && SendEmailUtil.sendEmail(makeEmailContent(seatId, tomorrow, false, "登录失败"));
        }
        System.out.println("1");
        // 获取 segement 参数
        HashMap<String, String> map1 = getViableTime(AreaCode, tomorrow);
        if (map1.get("status") == "0") {
            return emailInfo && SendEmailUtil.sendEmail(makeEmailContent(seatId, tomorrow, false, "获取segement参数失败"));
        }
        System.out.println("2");
        // 开始预约
        HashMap<String, String> res = seatReservation(map.get("accessToken"), TYPE, map1.get("segment"), seatId);
        if (res.get("status") == "1") {
            System.out.println("3");
            return emailInfo && SendEmailUtil.sendEmail(makeEmailContent(seatId, tomorrow, true, "莫等闲，白了少年头，空悲切！"));
        } else {
            System.out.println("4"+res.get("msg"));
            return emailInfo && SendEmailUtil.sendEmail(makeEmailContent(seatId, tomorrow, false, res.get("msg")));
        }
    }

    /**
     * 登录
     */
    public static HashMap<String, String> login() throws IOException {
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "0");
        JSONObject obj = requestPost(BASE_URL + "/login", "from=mobile&password=" + PASSWORD + "&username=" + USERNAME);
        if (obj.getInteger("status") == 1) {
            JSONObject infoObj = obj.getJSONObject("data").getJSONObject("list");
            map.replace("status", "1");
            map.put("accessToken", obj.getJSONObject("data").getJSONObject("_hash_").getString("access_token"));
        }
        return map;
    }

    /**
     * 预约
     */
    public static HashMap<String, String> seatReservation(String accessToken, String type, String segment, String seatId) throws IOException {
        String paramStr = "access_token=" + accessToken + "&userid=" + USERNAME + "&type=" + type + "&id=" + seatId + "&segment=" + segment;
        JSONObject obj = requestPost(BASE_URL + "/spaces/" + seatId + "/book", paramStr);
        HashMap<String, String> map = new HashMap<>();
        map.put("status", "0");
        if (obj.getInteger("status") == 1 && obj.getString("msg").indexOf("预约成功") != -1) {
            map.replace("status", "1");
        } else {
            map.put("msg", obj.getString("msg"));
        }
        return map;
    }

    /**
     * 获取区域对应的id字段
     */
    public static void getAreaInfo() throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/areas?tree=1");
        if (obj.getInteger("status") == 0) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        }
    }

    /**
     * 获取两个重要参数 segment 和 spaceId
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
     * 获取座位id，字段即为id
     */
    public static void getSeatInfo(String date, String endTime, String segment, String startTime) throws IOException {
        JSONObject obj = requestGet(BASE_URL + "/spaces_old?area=" + AreaCode + "&day=" + date + "&endTime=" + endTime + "&segment=" + segment + "&startTime=" + startTime);
        if (obj.getInteger("status") == 1) {
            System.out.println(obj.getJSONObject("data").getJSONArray("list"));
        } else {
            System.out.println("error");
        }
    }

    /**
     * 对多个座位进行检查，如果某个座位预约失败，则自动预约下一个座位
     */
    public static Boolean bookSeatLoop() throws Exception {
        for (int i=0;i<SEATS_ID.length;i++) {
            if (!beginSeatReservation(SEATS_ID[i])) {
                continue;
            } else {
                System.out.println("预约成功，座位id为：" + SEATS_ID[i]);
                return true;
            }
        }
        return false;
    }

    /**
     * 请求配置
     */
    public static JSONObject requestConfig(String oriUrl, String method, String paramsStr) throws IOException {
        URL url = new URL(oriUrl);
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; OXF-AN10 Build/HUAWEIOXF-AN10; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/107.0.5304.141 Mobile Safari/537.36 XWEB/5015 MMWEBSDK/20221206 MMWEBID/838 MicroMessenger/8.0.32.2300(0x2800205D) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64");
        // 伪造请求IP，防止请求失败，这个IP可以用学校所在地区的一个公网IP地址即可
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
            res = JSONObject.parseObject(stringBuffer.toString());
        } else {
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
     */
    public static JSONObject requestGet(String oriUrl) throws IOException {
        return requestConfig(oriUrl, "GET", null);
    }

    /**
     * POST请求
     */
    public static JSONObject requestPost(String oriUrl, String paramsStr) throws IOException {
        return requestConfig(oriUrl, "POST", paramsStr);
    }

    /**
     * 邮件文本
     */
    public static String makeEmailContent(String seatId, String dateStr, boolean success, String content) {
        return "<div>座位ID：" + seatId + "</div><div>预约日期：" + dateStr + "</div><div>预约结果：<font color=" + ((success==true) ? "\"green\">预约成功" : "\"red\">预约失败") + "</font></div><div>更多信息：" + content + "</div>";
    }
}
