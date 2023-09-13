/*
    THIS PROGRAM IS TO PARSE EARTHQUAKE INFORMATION FROM 
    https://www.cwb.gov.tw/V8/C/E/index.html
*/
import java.util.*;
import java.util.regex.*;

import java.time.format.DateTimeFormatter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.LocalDateTime;
import java.time.Duration;
// import java.time.LocalTime;


public class App {
    private static LocalDateTime current_timing;
    public static void main(String[] args) throws Exception {
        current_time();

        String website = "https://www.cwb.gov.tw/V8/C/E/MOD/MAP_LIST.html";
        Document doc = Jsoup.connect(website).get(); // html code of the website
        String eq_info = extract_eq_description(doc);
        String[] all_time = extract_eq_info(eq_info);
        String[] no_eq = {null};

        String eq_message;
        if (Arrays.equals(all_time, no_eq) == false){
            eq_message = message_content(all_time);
        }
    }

    private static void current_time(){
        /* Get current time and extract year, month, day, hour, minute, second information */
        LocalDateTime now = LocalDateTime.now();
        App.current_timing = now;
        System.out.println(App.current_timing);
    }

    private static String extract_eq_description(Document doc){
        /* Extract all 15 earthquake description from html. */
        String pattern_a_href = "<a href=.{1,100}title=\"檢視地震資訊\">";
        String body_tag_1 = "<body>";
        String body_tag_2 = "</body>";
        String content = doc.toString();
        int body_idx_1, body_idx_2;
        content = content.replaceAll(pattern_a_href, ""); // remove header of the XHR

        body_idx_1 = content.indexOf(body_tag_1);
        body_idx_2 = content.indexOf(body_tag_2);
        StringBuffer process_content = new StringBuffer();
        process_content.append(content);
        process_content.delete(body_idx_2, process_content.length());
        // remove things after </body>
        process_content.delete(0, body_idx_1 + body_tag_1.length());
        // remove things before <body>

        content = process_content.toString();
        content = content.trim(); // remove space at the beginning
        // System.out.println(content);
        return content;
    }

    private static String[] extract_eq_info(String eq_description){
        /* Extract time, location, and strength of a single earthquake. */
        String[] eq_msg = eq_description.split("</a> {0,2}");
        Pattern pattern_time = Pattern.compile("時間為[0-9]{2}月[0-9]{2}日[0-9]{2}時[0-9]{2}");
        Pattern pattern_place = Pattern.compile(".{2}[縣市]政府");
        Pattern pattern_eq_strength = Pattern.compile("地震規模[0-9].[0-9]");
        int eq_num = eq_msg.length;

        int year = Integer.valueOf(App.current_timing.getYear());
        long pass_min;
        String city;
        float eq_strength;
        boolean eq_happened = false;
        
        String[] eq_time_array = new String[eq_num];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime dateTime;
        for(int i = 0; i < eq_num; i++){
            Matcher match_time = pattern_time.matcher(eq_msg[i]);
            Matcher match_place = pattern_place.matcher(eq_msg[i]);
            Matcher match_eq_strength = pattern_eq_strength.matcher(eq_msg[i]);

            if (match_time.find()) {
                String time_extract = match_time.group(0).toString();
                int eq_month = Integer.valueOf(time_extract.substring(3, 5));
                if (eq_month >= Integer.valueOf(App.current_timing.getMonthValue())){
                    ;
                }
                else{
                    year = year - 1;
                }
                dateTime = LocalDateTime.of(
                    year,
                    Integer.valueOf(time_extract.substring(3, 5)),
                    Integer.valueOf(time_extract.substring(6, 8)),
                    Integer.valueOf(time_extract.substring(9, 11)),
                    Integer.valueOf(time_extract.substring(12, 14)),
                    0
                );
                Duration d = Duration.between(dateTime, App.current_timing);
                pass_min = d.toMinutes();
                if (pass_min > 3000){
                    continue;
                }
            }
            else {
                continue;
            }
            
            if (match_place.find()) {
                city = match_place.group(0).toString().substring(0, 3);
            }
            else {
                continue;
            }
            
            if (match_eq_strength.find()){
                String string_eq_strength = match_eq_strength.group(0).toString();
                int int_strength_str_len = string_eq_strength.length();
                string_eq_strength = string_eq_strength.substring(int_strength_str_len-3, int_strength_str_len);
                eq_strength = Float.parseFloat(string_eq_strength);
            }
            else {
                continue;
            }
            if (feel_eq(pass_min, city, eq_strength) == true){
                String formattedDateTime = dateTime.format(formatter);
                eq_time_array[i] = formattedDateTime;
                eq_happened = true;
            }
            else {
                continue;
            }
        }
        
        if (eq_happened == true){
            return eq_time_array;
        }
        else{
            String[] empty_array = {null};
            return empty_array;
        }
    }

    private static boolean feel_eq(long min, String place, float strength){
        /*Check if the earthquake is large enough to feel it.*/
        // Input:
        //  min: time
        //  place: city
        //  strength: int
        // Output:
        //  feel: t/f
        String[][] city_group = {
            {"臺北市", "新北市", "宜蘭縣", "基隆市"},
            {"臺北市", "新北市", "宜蘭縣", "基隆市", "桃園市", "新竹縣", "新竹市"},
            {"臺北市", "新北市", "宜蘭縣", "基隆市", "桃園市", "新竹縣", "新竹市", "花蓮縣", "臺東縣"},
        };
        boolean feel = false;
        
        if (Arrays.stream(city_group[0]).anyMatch(place::equals)){
            if (strength >= 2){
                System.out.println("1");
                feel = true;
            }
            else {
                ;
            }
        }
        else if (Arrays.stream(city_group[1]).anyMatch(place::equals)){
            if (strength >= 4){
                System.out.println("2");
                feel = true;
            }
            else {
                ;
            }
        }
        else if (Arrays.stream(city_group[2]).anyMatch(place::equals)){
            if (strength >= 4){
                System.out.println("3");
                feel = true;
            }
            else {
                ;
            }
        }
        else{
            System.out.println("\t\tother");
        }
        return feel;
    }

    private static String message_content(String[] time){
        /* The message that is gonna send to someone. */
        String message = "有地震，你有感覺到嗎？有跌倒嗎？有撞到嗎？有受傷嗎？";
        String time_set = "";
        for(int i=0; i<time.length; i++){
            if (time[i] == null){
                continue;
            }
            if (time_set == ""){
                time_set = time_set + time[i];
            }
            else{
                time_set = time_set + "、" + time[i];
            }
        }
        message = "剛剛" + time_set + message;
        System.out.println(message);
        return message;
    }
    
}
