package com.luyuan.yuanpicturebackend.utils;

public class ValidationUtils {

    /**
     * 验证电话号码是否合法
     * 支持格式:
     * - 国内手机号: 1开头的11位数字 (13xxxxxxxxx, 14xxxxxxxxx, 15xxxxxxxxx等)
     * - 国内座机: 区号-号码 (如: 010-12345678)
     * - 国际格式: +国家代码 号码 (如: +86 13812345678)
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // 去除所有空格
        phoneNumber = phoneNumber.replaceAll("\\s", "");

        // 国内手机号码正则
        String mobilePattern = "^1[3-9]\\d{9}$";

        // 国内座机号码正则 (区号-号码)
        String telPattern = "^0\\d{2,3}-\\d{7,8}$";

        // 国际电话号码正则 (+国家代码-号码)
        String internationalPattern = "^\\+\\d{1,4}[-\\d]{6,15}$";

        return phoneNumber.matches(mobilePattern) ||
               phoneNumber.matches(telPattern) ||
               phoneNumber.matches(internationalPattern);
    }

    /**
     * 验证邮箱是否合法
     * 基本符合RFC 5322标准
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // 邮箱正则表达式
        String emailPattern =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
            "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        return email.matches(emailPattern);
    }

    /**
     * 验证URL是否合法
     * 校验前会删除URL中的查询参数（?后面的内容）
     */
    public static boolean isValidWebsite(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 先删除?后面的查询参数
        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            url = url.substring(0, queryIndex);
        }

        // URL正则表达式 - 匹配http/https协议，域名格式，可选的端口和路径
        String urlPattern = "^(https?:\\/\\/)?" + // 协议 (http:// 或 https://)
                "((([a-zA-Z\\d]([a-zA-Z\\d-]*[a-zA-Z\\d])*)\\.)+[a-zA-Z]{2,}|" + // 域名
                "((\\d{1,3}\\.){3}\\d{1,3}))" + // 或 IP地址
                "(\\:\\d+)?(\\/[-a-zA-Z\\d%_.~+]*)*$"; // 端口和路径

        return url.matches(urlPattern);
    }


    public static void main(String[] args) {
        String phoneNumber = "38123456278";
        System.out.println(isValidPhoneNumber(phoneNumber));
        String email = "1211121@qq.com";
        System.out.println(isValidEmail(email));
        String url = "https://xiaoin.com.cn/create?utm_source=lenovo&sharerUserId=1791299207231987713";
        System.out.println(isValidWebsite(url));
    }
}
