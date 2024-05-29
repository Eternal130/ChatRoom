package org.eternal130;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class servers extends JFrame implements ListSelectionListener, ActionListener, Runnable {
    private ServerSocket ss = null;
    JPanel west = new JPanel(new BorderLayout());
    JPanel 面板=new JPanel();//容纳聊天内容、广播
    JTextArea 聊天框=new JTextArea(10,20);
    JScrollPane 滚动聊天框=new JScrollPane(聊天框);
    JTextField 输入框=new JTextField(20);
    JPanel 子面板=new JPanel();
    private JButton jbt = new JButton("踢出聊天室");
    private JButton jbt1 = new JButton("群发消息");
    DefaultListModel<String> dl = new DefaultListModel<String>();
    private ArrayList<servers.ChatThread> users = new ArrayList<servers.ChatThread>(); //容量能够动态增长的数组
    private JList<String> userList = new JList<String>(dl);//显示对象列表并且允许用户选择一个或多个项的组件。单独的模型 ListModel 维护列表的内容。

    JScrollPane 滚动 = new JScrollPane(userList);//容纳用户名列表

    public servers() throws HeadlessException, IOException {
        聊天框.setEditable(false);
        面板.setLayout(new BorderLayout());
        面板.add(滚动聊天框,BorderLayout.CENTER);
        面板.add(子面板,BorderLayout.SOUTH);
        子面板.setLayout(new BorderLayout());
        子面板.add(输入框,BorderLayout.NORTH);
        子面板.add(jbt,BorderLayout.EAST);
        子面板.add(jbt1,BorderLayout.WEST);
        //实现群发
        jbt1.addActionListener(this);
        //实现踢人
        jbt.addActionListener(this);
        west.setPreferredSize(new Dimension(100,150));//在使用了布局管理器后用setPreferredSize来设置窗口大小
        west.add(滚动,BorderLayout.CENTER);//显示好友列表

        this.setTitle("服务器");
        this.add(west,BorderLayout.EAST);
        this.add(面板,BorderLayout.CENTER);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocation(400,100);
        this.setSize(500, 400);
        this.setVisible(true);
        this.setAlwaysOnTop(true);
        ss = new ServerSocket(9999);
        new Thread(this).start();//监听用户端的加入
    }
    public void handleexpel(){
        sendMessage("OFFLINE#" + userList.getSelectedValuesList().get(0));
        dl.removeElement(userList.getSelectedValuesList().get(0));//更新defaultModel
        userList.repaint();//更新Jlist
//        滚动.repaint();
    }
    public void handleAll(){
        if(!输入框.getText().equals("")){
            sendMessage("ALL#" + 输入框.getText());
            //发送完后，使输入框中内容为空
            输入框.setText("");
        }
    }//群发消息
    @Override
    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if(label.equals("踢出聊天室")){
            handleexpel();
        } else if (label.equals("群发消息")) {
            handleAll();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {

    }

    public class ChatThread extends Thread{
        Socket s = null;
        private BufferedReader br = null;
        private PrintStream ps = null;
        public boolean canRun = true;
        String nickName = null;
        public ChatThread(Socket s) throws Exception{
            this.s = s;
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            ps = new PrintStream(s.getOutputStream());
        }
        public void run(){
            while(canRun){
                try{
                    String msg = br.readLine();//接收客户端发来的消息
                    String[] strs = msg.split("#");
                    switch (strs[0]) {
                        case "LOGIN" -> { //收到来自客户端的上线消息
                            nickName = strs[1];
                            dl.addElement(nickName);
                            userList.repaint();
                            sendMessage(msg);
                        }
                        case "MSG", "SMSG", "FSMSG" -> sendMessage(msg);
                        case "OFFLINE" -> { //收到来自客户端的下线消息
                            sendMessage(msg);
                            //System.out.println(msg);
                            dl.removeElement(strs[1]);
                            // 更新List列表
                            userList.repaint();
                        }
                        case "GPT" ->{
                            nickName = strs[1];
                            sendMessageGPT(msg,nickName);
                        }
                    }
                }catch (Exception ex){}
            }
        }
    }

    public void sendMessage(String msg){  //服务器端发送给所有用户
        for(servers.ChatThread ct : users){
            ct.ps.println(msg);
        }
        聊天框.append(msg+"\n");
    }
    public void sendMessageGPT(String msg,String name){  //服务器端发送给指定用户
        聊天框.append(msg+"\n");
        String[] strs = msg.split("#");
        String out = null;
        try {
            out = ApiTestA.input(strs[2]);
        } catch (Exception e) {
            out=e.toString();
        }
        String[] strss=out.split("\n");
        for(String str : strss){
            if(!str.equals("")){
                for(servers.ChatThread ct : users){
                    if(ct.nickName.equals(name))
                        ct.ps.println("GPT#"+name+"#"+str);
                }
                聊天框.append("GPT#"+name+"#"+str+"\n");
            }
        }
    }

    @Override
    public void run() {
        while(true){
            try{
                Socket s = ss.accept();
                servers.ChatThread ct = new servers.ChatThread(s); //为该客户开一个线程
                users.add(ct); //将每个线程加入到users
                //发送Jlist里的用户登陆信息，为了防止后面登陆的用户无法更新有前面用户的好友列表
                ListModel<String> model = userList.getModel();//获取Jlist的数据内容
                for(int i = 0; i < model.getSize(); i++){
                    ct.ps.println("USERS#" + model.getElementAt(i));
                }
                ct.start();
            }catch (Exception ex){
                ex.printStackTrace();
                javax.swing.JOptionPane.showMessageDialog(this,"服务器异常！");
                System.exit(0);
            }
        }

    }
    public static void main(String[] args) throws IOException {
        new servers();
    }
}
