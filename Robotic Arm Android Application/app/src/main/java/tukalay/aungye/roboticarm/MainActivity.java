package tukalay.aungye.roboticarm;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity
{
    final TextView[] DEGV=new TextView[6];
    TextView LOGs;
    Button SendData,YI,YD, XI,XD;
    SeekBar[] DSeB=new SeekBar[6];
    Dialog BTChoice, Config;

    private BluetoothAdapter Btadapter;
    private BluetoothSocket Btsocket= null;
    private BluetoothDevice Btdevice;

    static final UUID myuuid=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    float[] deg=new float[6];

    String log="";
    String btmainaddress="";
    String btconncetedname="";
    String MY_FILE="RoboticArm";
    String SaveFolder="",Username="",Password="";
    String[] Joint = {"Shoulder", "Base", "Elbow", "Gripper Patch"," Gripper Spin", "Gripper"};

    SharedPreferences datastore;

    long sleeptime=100;
    float l1,l2,l3,l4,v,h1,h2,a1;

    byte[] programlist;

    @Override
    public void onCreate(Bundle savedInstanceState)
	{

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        PrefInitialized();
        INIT();
        IsBtON();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mm=getMenuInflater();
        mm.inflate(R.menu.devicemenu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){

            case R.id.choosebtdevice:
                INIT();
                JoinToArm();
                break;

            case R.id.showsettings:
                Configure();
                break;

            case R.id.createserver:
                break;
        }
        return false;
    }

    private void IsBtON(){

        Btadapter=BluetoothAdapter.getDefaultAdapter();
        if(Btadapter==null){
            finish();

        }
        else {
            if(!Btadapter.isEnabled()){

                Intent ton=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(ton,1);
            }
        }
    }

    private void Configure(){

        PrefInitialized();
        Config = new Dialog(this);
        Config.setContentView(R.layout.settings);
        Config.setTitle("Configuration");
        int Ci[]={R.id.username,R.id.password,R.id.length1,R.id.length2,R.id.length3,R.id.length4,R.id.newlength,R.id.armhigh1,R.id.armhigh2,R.id.phase1,R.id.path};
        final EditText Cedt[]=new EditText[12];
        Button Save =(Button)Config.findViewById(R.id.save);

        for (int i=0;i<11;i++){

            Cedt[i]=(EditText)Config.findViewById(Ci[i]);
        }
        Cedt[0].setText(Username);
        Cedt[1].setText(Password);
        Cedt[2].setText(""+l1);
        Cedt[3].setText(""+l2);
        Cedt[4].setText(""+l3);
        Cedt[5].setText(""+l4);
        Cedt[6].setText(""+v);
        Cedt[7].setText(""+h1);
        Cedt[8].setText(""+h2);
        Cedt[9].setText(""+a1);
        Cedt[10].setText(SaveFolder);

        Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SharedPreferences.Editor  editor=datastore.edit();

                editor.putString("username",Cedt[0].getText().toString());
                editor.putString("password",Cedt[1].getText().toString());
                editor.putFloat("arm1",Float.parseFloat(Cedt[2].getText().toString()));
                editor.putFloat("arm2",Float.parseFloat(Cedt[3].getText().toString()));
                editor.putFloat("arm3",Float.parseFloat(Cedt[4].getText().toString()));
                editor.putFloat("gripper",Float.parseFloat(Cedt[5].getText().toString()));
                editor.putFloat("high1",Float.parseFloat(Cedt[6].getText().toString()));
                editor.putFloat("high2",Float.parseFloat(Cedt[7].getText().toString()));
                editor.putFloat("phase",Float.parseFloat(Cedt[8].getText().toString()));
                editor.putFloat("newLength",Float.parseFloat(Cedt[9].getText().toString()));
                editor.putString("saveFolder",Cedt[10].getText().toString());
                editor.apply();
            }
        });
        Config.show();

    }

    private  void JoinToArm(){

        IsBtON();

        BTChoice=new Dialog(this);
        BTChoice.setContentView(R.layout.choosedevice);
        BTChoice.setTitle("Choose Devices");
        ListView chooser=(ListView)BTChoice.findViewById(R.id.btdevices);

        ArrayList list = new ArrayList();
        final ArrayList address=new ArrayList();
        ArrayAdapter<String> btdevivelist;

        final Set<BluetoothDevice> btdevices=Btadapter.getBondedDevices();
        if(btdevices.size()>0){
            for(BluetoothDevice bt: btdevices){
                list.add(bt.getName()+"\n"+bt.getAddress());
                address.add(bt.getAddress());
            }

        }else {

        }
        btdevivelist=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_single_choice,list);
        chooser.setAdapter(btdevivelist);

        BTChoice.show();

        chooser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String st=address.get(i).toString();
                btconncetedname=((TextView)view).getText().toString();
                btmainaddress=st;
                BTHC btc=new BTHC();
                btc.execute(st);
                BTChoice.dismiss();

            }
        });
    }

    private void PrefInitialized(){
        datastore=getApplicationContext().getSharedPreferences(MY_FILE,MODE_PRIVATE);
        if(!datastore.contains("firstUser")){
            SharedPreferences.Editor  editor=datastore.edit();

            editor.putBoolean("firstUser",true);
            editor.putBoolean("paired",false);
            editor.putString("username","aungyehtet");
            editor.putString("password","password");
            editor.putString("saveFolder",Environment.getExternalStorageDirectory().toString());
            editor.putString("BTaddress","");
            editor.putFloat("arm1",60);
            editor.putFloat("arm2",60);
            editor.putFloat("arm3",60);
            editor.putFloat("gripper",60);
            editor.putFloat("high1",60);
            editor.putFloat("high2",60);
            editor.putFloat("phase",60);
            editor.putFloat("newLength",60);
            editor.putInt("MODE",0);
            editor.apply();


        }else{
            l1=datastore.getFloat("arm1",0f);
            l2=datastore.getFloat("arm2",0f);
            l3=datastore.getFloat("arm3",0f);
            l4=datastore.getFloat("gripper",0f);
            SaveFolder=datastore.getString("saveFolder","");
            v=datastore.getFloat("newLength",0f);
            h1=datastore.getFloat("high1",0f);
            h2=datastore.getFloat("high2",0f);
            a1=datastore.getFloat("phase",0f);
            Username=datastore.getString("username","");
            Password=datastore.getString("password","");

        }
    }

    public void INIT(){

        int[] TxVId={R.id.degvalue1,R.id.degvalue2,R.id.degvalue3,R.id.degvalue4,R.id.degvalue5,R.id.degvalue6};
        int[] SeBId={R.id.deg1,R.id.deg2,R.id.deg3,R.id.deg4,R.id.deg5,R.id.deg6};
        LOGs=(TextView)findViewById(R.id.logsout);
        SendData=(Button)findViewById(R.id.senddata);


        SendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpLog("A R1 "+coslaw(11,7,14)[0]);
                UpLog("B R2 "+coslaw(11,7,14)[1]);
                UpLog("C R3 "+coslaw(11,7,14)[2]);




            }
        });

        YI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        YD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        XI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        XD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        LOGs.setText("ok");

        for (int i=0;i<DEGV.length;i++){
           final int iop=i;
            DEGV[i]=(TextView)findViewById(TxVId[i]);
            DSeB[i]=(SeekBar)findViewById(SeBId[i]);
            DSeB[i].setMax(180);
            DSeB[i].setProgress(90);
            deg[0]=100;
            deg[1]=90;
            deg[2]=169;
            deg[3]=10;
            deg[4]=81;
            deg[5]=84;

            DSeB[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int pv, boolean b) {

                    DEGV[iop].setText(Joint[iop]+": "+pv+"");
                    deg[iop]=pv;
                    if(Btsocket!=null){
                        ClrLog();
                        UpLog(Joint[0]+" "+deg[0]+"\n"+Joint[1]+" "+deg[1]+"\n"+Joint[2]+" "+deg[2]+"\n"+Joint[3]+" "+deg[3]+"\n"+Joint[4]+" "+deg[4]+"\n"+Joint[5]+" "+deg[5]);
                        SendDegs();

                    }
                    else {
                        ClrLog();
                        UpLog("Null Bt");
                    }

            }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    public class  RunProgram extends AsyncTask<Void,int[],Void>{
        int setbar[]=new int[7];

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

                for (int i=0; i<180;i++){


                    try {
                        Thread.sleep(90);

                    }catch (InterruptedException d){
                        UpLog(d.getMessage());
                    }
                    deg[0]=i;
                  publishProgress(setbar);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(int[]... values) {
            super.onProgressUpdate(values);
            DSeB[0].setProgress((int)deg[0]);
            ClrLog();
        }
    }

    public  class BTHC extends  AsyncTask<String,String,Void> {
        boolean showdelay = false;

        Vibrator vl = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {
            Btdevice = Btadapter.getRemoteDevice(strings[0]);

            try {
                Btsocket = Btdevice.createInsecureRfcommSocketToServiceRecord(myuuid);
                publishProgress("Connecting. . . to " + strings[0]);
                Btadapter.cancelDiscovery();
                Btsocket.connect();
                vl.vibrate(250);

                BufferedReader reader;
                InputStream io = Btsocket.getInputStream();
                reader = new BufferedReader(new InputStreamReader(io));

                String tempmes = "";
                publishProgress("Reading. . . . ");
                while (true) {
                    showdelay = true;
                    tempmes = reader.readLine();
                    publishProgress(tempmes);

                }

            } catch (IOException p) {
                showdelay = false;
                publishProgress("errr" + p.getMessage());
                log = "";
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            UpLog(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private void UpLog(String Mes){
        log=Mes+"\n"+log;

        LOGs.setText(log);
    }

    private void ClrLog(){
        log="log: ";
        LOGs.setText(log);
    }

    private void SendDegs(){

        byte[] tmpdatac=new byte[8];
        tmpdatac[0]=(byte)'N';
        tmpdatac[1]=(byte)(int)deg[0];
        tmpdatac[2]=(byte)(int)deg[1];
        tmpdatac[3]=(byte)(int)deg[2];
        tmpdatac[4]=(byte)(int)deg[3];
        tmpdatac[5]=(byte)(int)deg[4];
        tmpdatac[6]=(byte)(int)deg[5];
        tmpdatac[7]=(byte)'0';

        SendPrograms(tmpdatac);
    }

    private void AnalyticArm(){


    }
    private double[] coslaw(double A, double B, double C){
        double [] result = new double[3];

        result[2]=Math.toDegrees(Math.acos(((A*A)+(B*B)-(C*C))/(2*A*B)));
        result[1]=Math.toDegrees(Math.acos(((A*A)+(C*C)-(B*B))/(2*A*C)));
        result[0]=Math.toDegrees(Math.acos(((B*B)+(C*C)-(A*A))/(2*B*C)));
        return result;
    }

    private void SendPrograms(byte[] sendedData){

        if(sendedData.length==8){
            if(Btsocket!=null){
                try{

                    OutputStream outputStream=Btsocket.getOutputStream();
                    outputStream.write(sendedData);
                   UpLog("Programs are Sended");

                }catch (IOException o){

                    UpLog("Programs Send fail "+o.getMessage());
                }
            }else {

                UpLog("Programs Send fail");
            }
        }else {
            UpLog("more X");
        }
    }

}


