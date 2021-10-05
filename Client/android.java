public class CommsActivity extends AppCompatActivity {

    public static BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final String TAG = "CommsActivity";
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    //스위치
    public com.kyleduo.switchbutton.SwitchButton autobutton;
    //수동시 보이는 버튼
    public Button open;
    public Button close;
    // 보낼 애들
    public String conSt="connect";//커넥트 문자열
    public String autoCheck="0";//자동->0, 수동->1
    public String command="close";//열림 닫힘

    //받을 애들
    public String mise="";//미세먼지
    public String temp="";//온도
    public String hum="";//습도

    public String address;

    public String waitingMes;//1면 기다려야함 0면 걍 진행

    public class ConnectThread extends Thread {
        private ConnectThread(BluetoothDevice device) throws IOException {
            /*if (mmSocket != null) {
                if(mmSocket.isConnected()) {
                    send();
                }
            }*/
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
                tmp = mmDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            BTAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                Log.v(TAG, "Connection exception!");
                try {
                    mmSocket.close();
                    /*mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                    mmSocket.connect();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } */
                } catch (IOException closeException) {

                }
            }
            send();
        }

        public void send() throws IOException {

            //여기에 연결문자, 자동인지 수동인지, 열림인지 닫힘인지 split 문자는 *로 "연결문자(connect)*자동수동여부(0 자동,1 수동)*열림닫힘(open, close)"
            String msg = conSt+"*"+autoCheck+"*"+command;
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg.getBytes());
            Log.d(TAG, "보냈음~ voltage");
            receive();
        }

        public void receive() throws IOException {
            InputStream mmInputStream = mmSocket.getInputStream();
            byte[] buffer = new byte[256];
            int bytes;

            try {
                bytes = mmInputStream.read(buffer);
                //구분자 *로 스플릿 수행하는데 미세먼지, 온습도 받음 "미세먼지*온도*습도"
                String readMessage = new String(buffer, 0, bytes);
                String[] array=readMessage.split("\\*");
                mise=array[0];
                temp=array[1];
                hum=array[2];
                waitingMes=array[3];
                Log.d(TAG, "창문상태: " + waitingMes);
                if(waitingMes.equals("0")){//진행 가능상태
                Log.d(TAG, "Received: " + readMessage);
                TextView voltageLevel = (TextView) findViewById(R.id.message);
                voltageLevel.setText("미세먼지 농도: " + mise + " ㎍/㎥"+"\n"+"온도: "+temp+"℃"+"\n"+"습도: "+hum+"%");}
                else{//사용자가 기다려야 하는 상태
                    Toast.makeText(getApplicationContext(),"창문 상태가 변동 중입니다. 잠시후 시도해 주세요.",Toast.LENGTH_SHORT).show();
                }
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Problems occurred!");
                return;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comms);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Intent intent = getIntent();
        address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        Button voltButton = (Button) findViewById(R.id.measVoltage);
        open=(Button) findViewById(R.id.open);
        close=(Button) findViewById(R.id.close);
        open.setVisibility(View.INVISIBLE);
        close.setVisibility(View.INVISIBLE);
        BluetoothDevice device = BTAdapter.getRemoteDevice(address);
        //연결
        voltButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    new ConnectThread(device).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        });
        //자동 수동 스위치 버튼
        autobutton = (com.kyleduo.switchbutton.SwitchButton)findViewById(R.id.sb_use_listener);

        open.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                command="open";

                try {
                    new ConnectThread(device).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                command="close";

                try {
                    new ConnectThread(device).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        autobutton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){  //수동
                    autoCheck="1";
                    //버튼을 보이게 적용

                    open.setVisibility(View.VISIBLE);
                    close.setVisibility(View.VISIBLE);


                }else{  //자동
                    autoCheck="0";
                    open.setVisibility(View.INVISIBLE);
                    close.setVisibility(View.INVISIBLE);
                }
                try {
                    new ConnectThread(device).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}