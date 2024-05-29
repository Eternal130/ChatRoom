package org.eternal130;

import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import com.unfbx.chatgpt.interceptor.OpenAILogger;
import com.unfbx.chatgpt.interceptor.OpenAiResponseInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApiTestA {
    public static String out= null;
    public static String input(String input)throws Exception{
        //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8889));
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(new OpenAILogger());
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .proxy(/*proxy*/null)
//                .addInterceptor(httpLoggingInterceptor)
                .addInterceptor(new OpenAiResponseInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        OpenAiClient openAiClient = OpenAiClient.builder().apiHost("http://----.--/")
                .apiKey(Arrays.asList("sk-...."))
                .okHttpClient(okHttpClient)
                .build();

        List<Message> messages = new ArrayList<>();
        Message message = Message.builder().role(Message.Role.USER).content(input).build();
        messages.add(message);
        ChatCompletion chatCompletion = ChatCompletion.builder().messages(messages).build();
        ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
//    System.out.println(chatCompletionResponse);
        chatCompletionResponse.getChoices().forEach(e -> {
            out=(e.getMessage().getContent());
        });
        return out;
    }
}
