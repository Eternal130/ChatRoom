package org.eternal130;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class clients extends JFrame implements ListSelectionListener, ActionListener,Runnable {
    JPanel 面板=new JPanel();//容纳聊天内容、广播
    JPanel west = new JPanel(new BorderLayout());
    JTextArea 聊天框=new JTextArea(10,20);
    JTextArea 私聊框=new JTextArea(11,45);
    JScrollPane 滚动聊天框=new JScrollPane(聊天框);
    JScrollPane 滚动私聊框=new JScrollPane(私聊框);
    JTextField 输入框=new JTextField(24);
    JTextField 私聊输入框=new JTextField(25);
    JPanel 子面板=new JPanel();

    JButton jButton = new JButton("发消息");
    private JButton jbt = new JButton("发送消息");
    private JButton jbt1 = new JButton("私发消息");
    private JButton jbt2 = new JButton("发给服务器");
    DefaultListModel<String> dl = new DefaultListModel<String>();
    private JList<String> userList = new JList<String>(dl);//显示好友列表并且允许用户选择一个或多个项的组件。单独的模型 ListModel 维护列表的内容。
    JScrollPane 滚动 = new JScrollPane(userList);//容纳用户名列表
    private String nickName=null;
    private BufferedReader br = null;
    private PrintStream ps = null;

    boolean FirstSecret = true;//是否第一次私聊
    String sender=null;//私聊发送者的名字
    String receiver=null;//私聊接收者的名字

    double MAIN_FRAME_LOC_X;//父窗口x坐标
    double MAIN_FRAME_LOC_Y;//父窗口y坐标
    String suser = new String();

    public clients() throws IOException {
        聊天框.setEditable(false);
        面板.setLayout(new BorderLayout());
        面板.add(滚动聊天框,BorderLayout.CENTER);
        面板.add(子面板,BorderLayout.SOUTH);
        子面板.setLayout(new BorderLayout());
        子面板.add(输入框,BorderLayout.NORTH);
        子面板.add(jbt,BorderLayout.EAST);
        子面板.add(jbt1,BorderLayout.WEST);
        子面板.add(jbt2,BorderLayout.CENTER);
        west.setPreferredSize(new Dimension(100,150));//在使用了布局管理器后用setPreferredSize来设置窗口大小
        west.add(滚动,BorderLayout.CENTER);//显示好友列表
        nickName = JOptionPane.showInputDialog("用户名：");
        this.setTitle(nickName + "的聊天室");
        this.add(west,BorderLayout.EAST);
        this.add(面板,BorderLayout.CENTER);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocation(400,100);
        this.setSize(500, 400);
        this.setVisible(true);
        this.setAlwaysOnTop(true);
        //鼠标事件，点击
        jbt.addActionListener(this);
        jbt1.addActionListener(this);
        jbt2.addActionListener(this);
        Socket s = new Socket("127.0.0.1", 9999);
        br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        ps = new PrintStream(s.getOutputStream());
        new Thread(this).start();//run()
        ps.println("LOGIN#" + nickName);//发送登录信息，消息格式：LOGIN#nickName

        输入框.setFocusable(true);//设置焦点

        //键盘事件，实现当输完要发送的内容后，直接按回车键，实现发送
        //监听键盘相应的控件必须是获得焦点（focus）的情况下才能起作用
        输入框.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ps.println("MSG#" + nickName + "#" +  输入框.getText());//发送消息的格式：MSG#nickName#message
                    //发送完后，是输入框中内容为空
                    输入框.setText("");
                }
            }
        });

        //私聊消息框按回车发送消息
        私聊输入框.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleSS();
                }
            }
        });

        //监听系统关闭事件，退出时给服务器端发出指定消息
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ps.println("OFFLINE#" + nickName);//发送下线信息，消息格式：OFFLINE#nickName
            }
        });

        this.addComponentListener(new ComponentAdapter() {//监听父窗口大小的改变
            public void componentMoved(ComponentEvent e) {
                Component comp = e.getComponent();
                MAIN_FRAME_LOC_X = comp.getX();
                MAIN_FRAME_LOC_Y = comp.getY();
            }
        });
    }


    @Override
    public void valueChanged(ListSelectionEvent e) {

    }
    public void run(){//客户端与服务器端发消息的线程
        while (true){
            try{
                String msg=br.readLine();
                String[] strs = msg.split("#");
                //判断是否为服务器发来的登陆信息
                if(strs[0].equals("LOGIN")){
                    if(!strs[1].equals(nickName)){//不是本人的上线消息就显示，本人的不显示
                        聊天框.append(strs[1] + "上线啦！\n");
                        dl.addElement(strs[1]);//DefaultListModel来更改JList的内容
                        userList.repaint();
                    }
                }else if(strs[0].equals("GPT")){//接到服务器发送消息的信息
                    聊天框.append("系统回复:" + strs[2] + "\n");

                }else if(strs[0].equals("MSG")){//接到服务器发送消息的信息
                    if(!strs[1].equals(nickName)){//别人说的
                        聊天框.append(strs[1] + "说：" + strs[2] + "\n");
                    }else{
                        聊天框.append("我说：" + strs[2] + "\n");
                    }
                }else if(strs[0].equals("USERS")){//USER消息，为新建立的客户端更新好友列表
                    dl.addElement(strs[1]);
                    userList.repaint();
                }else if(strs[0].equals("ALL")){
                    聊天框.append("系统消息：" + strs[1] + "\n");
                }else if(strs[0].equals("OFFLINE")){
                    if(strs[1].equals(nickName)) {//如果是自己下线的消息，说明被服务器端踢出聊天室，强制下线
                        javax.swing.JOptionPane.showMessageDialog(this, "您已被系统请出聊天室！");
                        System.exit(0);
                    }
                    聊天框.append(strs[1] + "下线啦！\n");
                    dl.removeElement(strs[1]);
                    userList.repaint();
                }else if((strs[2].equals(nickName) || strs[1].equals(nickName)) && strs[0].equals("SMSG")){
                    if(!strs[1].equals(nickName)){
                        私聊框.append(strs[1] + "说：" + strs[3] + "\n");
                        聊天框.append("系统提示：" + strs[1] + "私信了你" + "\n");
                    }else{
                        私聊框.append("我说：" + strs[3] + "\n");
                    }
                }else if((strs[2].equals(nickName) || strs[1].equals(nickName)) && strs[0].equals("FSMSG"))
                {
                    sender = strs[1];
                    receiver = strs[2];
                    //接收方第一次收到私聊消息，自动弹出私聊窗口
                    if(!strs[1].equals(nickName)) {
                        FirstSecret = false;
                        私聊框.append(strs[1] + "说：" + strs[3] + "\n");
                        聊天框.append("系统提示：" + strs[1] + "私信了你" + "\n");
                        handleSec(strs[1]);
                    }
                    else {
                        私聊框.append("我说：" + strs[3] + "\n");
                    }
                }
            }catch (Exception ex){//如果服务器端出现问题，则客户端强制下线
                javax.swing.JOptionPane.showMessageDialog(this, "您已被系统请出聊天室！");
                System.exit(0);
            }
        }
    }

    public void handleSec(String name){ //建立私聊窗口
        JFrame jFrame = new JFrame();//新建了一个窗口
        JPanel JPL = new JPanel();
        JPanel JPL2 = new JPanel();
        FlowLayout f2 = new FlowLayout(FlowLayout.LEFT);
        JPL.setLayout(f2);
        JPL.add(私聊输入框);
        JPL.add(jButton);
        JPL2.add(滚动私聊框,BorderLayout.CENTER);
        JPL2.add(JPL,BorderLayout.SOUTH);
        jFrame.add(JPL2);
        jFrame.setAlwaysOnTop(true);

        jButton.addActionListener(this);
        私聊框.setEditable(false);
        私聊框.setFont(new Font("宋体", Font.PLAIN,15));
        jFrame.setSize(400,310);
        jFrame.setLocation((int)MAIN_FRAME_LOC_X+20,(int)MAIN_FRAME_LOC_Y+20);//将私聊窗口设置总是在父窗口的中间弹出
        jFrame.setTitle("与" + name + "私聊中");
        jFrame.setVisible(true);

        私聊输入框.setFocusable(true);//设置焦点

        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                私聊框.setText("");
                FirstSecret = true;
            }
        });
    }//私聊窗口

    @Override
    public void actionPerformed(ActionEvent e) {//鼠标点击事件
        String label = e.getActionCommand();
        if(label.equals("发送消息")){//群发
            handleSend();
        }else if(label.equals("私发消息") && !userList.isSelectionEmpty()){//未点击用户不执行
            suser = userList.getSelectedValuesList().get(0);//获得被选择的用户
            handleSec(suser);//创建私聊窗口
            sender = nickName;
            receiver = suser;
        }else if(label.equals("发消息")){
            handleSS();//私发消息
        }else if(label.equals("发给服务器")){
            handleSendGPT();//私发消息
        }else{
            System.out.println("不识别的事件");
        }
    }

    public void handleSS(){//在私聊窗口中发消息
        String name=sender;
        if(sender.equals(nickName)) {
            name = receiver;
        }
        if(FirstSecret) {
            ps.println("FSMSG#" + nickName + "#" + name + "#" + 私聊输入框.getText());
            私聊输入框.setText("");
            FirstSecret = false;
        }
        else {
            ps.println("SMSG#" + nickName + "#" + name + "#" + 私聊输入框.getText());
            私聊输入框.setText("");
        }
    }

    public void handleSend(){//群发消息
        //发送信息时标识一下来源
        ps.println("MSG#" + nickName + "#" +  输入框.getText());
        //发送完后，是输入框中内容为空
        输入框.setText("");
    }
    public void handleSendGPT(){//群发消息
        //发送信息时标识一下来源
        ps.println("GPT#" + nickName + "#" +  输入框.getText());
        聊天框.append("我向服务器说：" + 输入框.getText() + "\n");
        //发送完后，是输入框中内容为空
        输入框.setText("");
    }


    public static void main(String[] args) throws IOException {
        new clients();
    }

}
