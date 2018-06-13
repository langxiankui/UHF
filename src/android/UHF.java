package cordova.plugin.UHF;

import android.content.SharedPreferences;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.telecom.Call;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;

import com.zistone.uhf.ZstUHFApi;
import com.zistone.uhf.ZstCallBackListen;
import com.uhf.api.EPC;
import com.uhf.api.Util;


/**
 * This class echoes a string called from JavaScript.
 */
public class UHF extends CordovaPlugin {

    private String result;


    private final String TAG = "ZstUHFApi";
    private Button button_start;
    private ZstUHFApi mZstUHFApi;
    private RelativeLayout tab_inventory, tab_setting, tab_rw;
    private TextView textViewEPC, tag_count;
    private boolean in_set_param = false, isStart = false;
    private int button_setting_num = 0;
    private String lostTag = "";
    private ListView listViewData;
    private SharedPreferences sp;
    private ArrayList<EPC> listEPC;
    private ArrayList<Map<String, Object>> listMap;
    private String ss;
    private SoundPool soundPool;
    private final int STATE_NO_THING = 0, STATE_START_INVENTORY = 1,
            STATE_SET_POWER = 2, STATE_GET_POWER = 3,
            STATE_SET_CHANNEL = 4, STATE_GET_CHANNEL = 5,
            STATE_SET_PARAM = 6, STATE_GET_PARAM = 7,
            STATE_READ_TAG = 8, STATE_WRITE_TAG = 9,
            STATE_SET_SELECT = 10;


    private int m_opration = STATE_NO_THING;

    private int gpio1_num = 81, gpio2_num = 113;
    private String SerialName = "/dev/ttyHSL1";


    //-----------------//

    private Spinner spinnerMemBank;// 数据区
    private EditText editPassword;// 密码
    private EditText editAddr;// 起始地址
    private EditText editLength;// 读取的数据长度
    private EditText editWriteData;// 要写入的数据
    private EditText editReadData;// 读取数据展示区
    private final String[] strMemBank = {"RESERVE", "EPC", "TID", "USER"};//USER分别对应0,1,2,3
    private ArrayAdapter<String> adatpterMemBank;
    private int membank;// 数据区
    private int addr = 0;// 起始地址
    private int length = 6;// 读取数据的长度

    //-----------------------//

    private TextView tv_info;
    private EditText edittext_pwr, edittext_thrd, edittext_SelParam, edittext_ptr, edittext_mask;
    private Spinner sp_country_set, sp_mixe_set, sp_ifamp_set, sp_truncate_set;
    private ArrayAdapter<String> sp_country_adapter, sp_mixe_adapter, sp_ifamp_adapter, sp_truncate_adapter;
    private int curArrValue;
    private String[] countryArr = {"中国900MHz", "中国800MHz", "美国", "欧洲", "韩国"};
    private byte[] countryArrValue = new byte[]{0x01, 0x04, 0x02, 0x03, 0x06};

    private int curMixeValue;
    private String[] MixerArr = {"0db", "3db", "6db", "9db", "12db", "15db", "16db"};
    private byte[] MixerArrValue = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06};

    private int curTruncateValue = 0x00;
    private String[] TruncateArr = {"Disable truncation", "Enable truncation"};

    private int curIfampValue;
    private String[] IfampArr = {"12db", "18db", "21db", "24db", "27db", "30db", "36db", "40db"};
    private byte[] IfampArrValue = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
    private int mThrd = 0;
    private int mSelParam;
    private byte[] mPtr;
    private byte[] mMask;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        String model = android.os.Build.MODEL;
        if (model.contains("msm8953")) {
            gpio1_num = 66;
            gpio2_num = 98;
            SerialName = "/dev/ttyHSL0";
        }
        sp = cordova.getActivity().getSharedPreferences("UHF_SHRAE", MODE_PRIVATE);
        button_setting_num = 0;
        mZstUHFApi = ZstUHFApi.getInstance(cordova.getActivity(), new MyZstUhfListen());
        listEPC = new ArrayList<EPC>();
        mZstUHFApi.setModelPower(true, gpio1_num, gpio2_num);
        openScanDevice();
    }


    private void onDataReceived(final byte[] buffer, final int size) {
        ss = lostTag;
        int endTag = 1;
        for (int i = 0; i < size; i++) {
            String oneByte = Util.toHex(buffer[i]);
            if (oneByte.equals("BB")) {
                if (endTag == 1)
                    ss = Util.toHex(buffer[i]) + " ";
                else {
                    ss += Util.toHex(buffer[i]) + " ";
                    endTag = 0;
                }
            } else {
                ss += Util.toHex(buffer[i]) + " ";
                endTag = 0;
            }
            if (oneByte.equals("7E")) {
                endTag = 1;
                if (true == Util.checkSum(ss)) {
                    Log.d(TAG, "m_opration = " + m_opration);
                    if (m_opration == STATE_START_INVENTORY) {
                        if (ss.length() > 52) {
                            String data = ss.trim().replaceAll(" ", "");
                            int pl_h = Util.toInt(data.substring(6, 8));
                            int pl_l = Util.toInt(data.substring(8, 10));
                            int plen = (pl_h * 256 + pl_l) * 2;
                            int RSSI = Util.toInt(data.substring(10, 12));
                            int pc_h = Util.toInt(data.substring(12, 14));
                            int pc_l = Util.toInt(data.substring(14, 16));
                            int pc = pc_h * 256 + pc_l;
                            String epc = "";
//							Log.d(TAG, "data.length() = "+ data.length());
//							Log.d(TAG, "plen = "+ plen);
                            if (plen >= 10 && data.length() >= 16 + plen - 10) {
                                epc = data.substring(16, 16 + plen - 10);
                                Log.d(TAG, "epc = " + epc);
                            }
                            soundPool.play(1, 1, 1, 0, 0, 1);
                        } else {// EPC不够
                            int len = Util
                                    .toInt(ss.substring(12, 14));
                            if (len > 5) {
                                len = len - 5;
                                try {
                                    Thread.sleep(1); // yinbo remove
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    cordova.getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            String recvStr = ss.trim().replaceAll(" ", "");
                            switch (m_opration) {
                                case STATE_SET_POWER:// 设置功率
                                    if (recvStr.substring(4, 6).equals("B6")) {
//                    tv_info.setText(getString(R.string.set_power_success));
                                    } else {
//                    tv_info.setText(getString(R.string.set_power_fail));
                                    }
                                    break;
                                case STATE_GET_POWER:// 获取功率
                                    if (recvStr.substring(4, 6).equals("B7")) {
                                        result = Util.GetPower(ss.trim());
//                    tv_info.setText(getString(R.string.get_power_success));
//                    edittext_pwr.setText(Util.GetPower(ss.trim()));
                                    } else {
//                    tv_info.setText(getString(R.string.get_power_fail));
                                    }
                                    break;
                                case STATE_SET_CHANNEL://设置工作信道
                                    if (recvStr.substring(4, 6).equals("AB")) {
//                    tv_info.setText(getString(R.string.set_channel_success));
                                    } else {
//                    tv_info.setText(getString(R.string.set_channel_fail));
                                    }
                                    break;
                                case STATE_GET_CHANNEL://设置工作信道
                                    if (recvStr.substring(4, 6).equals("AA")) {
//                    tv_info.setText(getString(R.string.get_channel_success));
                                    } else {
//                    tv_info.setText(getString(R.string.get_channel_fail));
                                    }
                                    break;
                                case STATE_SET_PARAM://设置模块参数
                                    if (recvStr.substring(4, 6).equals("F0")) {
//                    tv_info.setText(getString(R.string.set_param_success));
                                    } else {
//                    tv_info.setText(getString(R.string.set_param_fail));
                                    }
                                    break;
                                case STATE_GET_PARAM://获取模块参数
                                    if (recvStr.substring(4, 6).equals("F1")) {
//                    tv_info.setText(getString(R.string.get_param_success));
                                        if (recvStr.length() >= 12) {
                                            curMixeValue = Integer.parseInt(recvStr.substring(10, 12));
//                      sp_mixe_set.setSelection(curMixeValue);
                                        }
                                        if (recvStr.length() >= 14) {
                                            curIfampValue = Integer.parseInt(recvStr.substring(12, 14));
//                      sp_ifamp_set.setSelection(curIfampValue);
                                        }
                                        if (recvStr.length() >= 18) {
                                            String thrd_str = recvStr.substring(14, 18);
                                            if (thrd_str != null) {
                                                Log.d(TAG, "thrd_str = " + thrd_str);
                                                if (recvStr.length() >= 4) {
                                                    int thred_h = Util.toInt(thrd_str.substring(0, 2));
                                                    int thred_l = Util.toInt(thrd_str.substring(2, 4));
                                                    Log.d(TAG, "thred_h = " + thred_h);
                                                    Log.d(TAG, "thred_l = " + thred_l);
                                                    mThrd = thred_h * 256 + thred_l;
                                                    Log.d(TAG, "mThrd = " + mThrd);
//                          edittext_thrd.setText(String.valueOf(mThrd));
                                                }
                                            }
                                        }
                                    } else {
//                    tv_info.setText(getString(R.string.get_param_fail));
                                    }
                                    break;
                                case STATE_READ_TAG:// 读标签
                                    if (recvStr.substring(4, 6).equals("39")) {
                                        // 读取成功
//                    textViewEPC.setText(recvStr.substring(8 * 2, (8 + 12) * 2));
//                    editReadData.append("读取:"
//                      + recvStr.substring(recvStr.length()
//                        - 4 - length * 4,
//                      recvStr.length() - 4) + "\n");
                                        result = recvStr.substring(recvStr.length() - 4 - length * 4,
                                                recvStr.length() - 4);
                                    } else {
                                        // 读取失败
//                    editReadData.append("读取失败 \n");
                                        Log.d("result", "读取失败");
                                    }
                                    break;
                                case STATE_WRITE_TAG:// 写标签
                                    if (recvStr.substring(4, 6).equals("49")) {
                                        // 读取成功
//                    editReadData.append("写入成功" + "\n");
                                    } else {
                                        // 读取失败
//                    editReadData.append("写入失败 \n");
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "checkSum if fail!!!");
                }
            }
        }
        lostTag = ss;
    }

    public class MyZstUhfListen implements ZstCallBackListen {
        @Override
        public void onUhfReceived(byte[] data, int len) {
            // TODO Auto-generated method stub
            onDataReceived(data, len);
        }
    }


    private int openScanDevice() {
        String path = sp.getString("DEVICE", SerialName);
        int baud_rate = Integer.decode(sp.getString("BAUDRATE", "115200"));
        int data_bits = Integer.decode(sp.getString("DATA", "8"));
        int stop_bits = Integer.decode(sp.getString("STOP", "1"));
        int flow = 0;
        int parity = 'N';
        String flow_ctrl = sp.getString("FLOW", "None");
        String parity_check = sp.getString("PARITY", "None");
        Log.d(TAG, "baud_rate = " + baud_rate);
        /* Check parameters */
        if ((path.length() == 0) || (baud_rate == -1)) {
            throw new InvalidParameterException();
        }
        Log.d(TAG, "path = " + path);
        if (flow_ctrl.equals("RTS/CTS"))
            flow = 1;
        else if (flow_ctrl.equals("XON/XOFF"))
            flow = 2;

        if (parity_check.equals("Odd"))
            parity = 'O';
        else if (parity_check.equals("Even"))
            parity = 'E';

        int retOpen = -1;
        if (mZstUHFApi != null) {
            retOpen = mZstUHFApi.opendevice(
                    new File(path), baud_rate, flow,
                    data_bits, stop_bits, parity, gpio1_num);
        }
        Log.d(TAG, "retOpen = " + retOpen);
//		btn_scan_one.setEnabled(false);
//		btn_scan_continuous.setEnabled(false);
        if (retOpen == android.serialport.SerialPortManager.RET_OPEN_SUCCESS ||
                retOpen == android.serialport.SerialPortManager.RET_DEVICE_OPENED) {
//			btn_scan_one.setEnabled(true);
//			btn_scan_continuous.setEnabled(true);
//			isOpened = true;
        } else if (retOpen == android.serialport.SerialPortManager.RET_NO_PRTMISSIONS) {
//      DisplayError(R.string.error_security);
        } else if (retOpen == android.serialport.SerialPortManager.RET_ERROR_CONFIG) {
//      DisplayError(R.string.error_configuration);
        } else {
//      DisplayError(R.string.error_unknown);
        }
        return retOpen;
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        } else if (action.equals("readCard")) {
            this.readCard(args, callbackContext);
            return true;
        } else if (action.equals("searchCard")) {
            this.searchCard(callbackContext);
            return true;
        } else if (action.equals("writeCard")) {
            this.writeCard(args, callbackContext);
            return true;
        } else if (action.equals("getPower")) {
            this.getPower(callbackContext);
            return true;
        } else if (action.equals("setPower")) {
            this.setPower(args, callbackContext);
            return true;
        } else if (action.equals("getParam")) {
            this.getParam(callbackContext);
            return true;
        } else if (action.equals("setParam")) {
            this.setParam(args, callbackContext);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void searchCard(CallbackContext callbackContext) {
        m_opration = STATE_READ_TAG;
        result = "";
        mZstUHFApi.readCradTag(Util.hexStr2Str("00000000"), (byte) 1, 2, 6);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callbackContext.success(result);
            }
        }, 200);
    }

    private void readCard(JSONArray message, CallbackContext callbackContext) throws JSONException {

        m_opration = STATE_READ_TAG;
        result = "";
        int site = message.getJSONObject(0).getInt("site");
        length = message.getJSONObject(0).getInt("length");
        int addr = 0;
        if (site == 1) {
            addr = 2;
            length = 6;
        } else if (site == 3) {
            addr = 0;
        }
        mZstUHFApi.readCradTag(Util.hexStr2Str("00000000"), (byte) site, addr, length);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if ("".equals(result) || result.equals("读取失败")) {
                    callbackContext.error("读取失败");
                } else {
                    if (site == 1) {
                        callbackContext.success(result);
                    } else if (site == 3) {
                        callbackContext.success(Util.hexString2Str(result));
                    }
                }
            }
        }, 200);
    }

    private void writeCard(JSONArray message, CallbackContext callbackContext) throws JSONException {

        m_opration = STATE_WRITE_TAG;
        String _data = message.getJSONObject(0).getString("data");
        String reg = "^[\\x20-\\x7e]+$";
        if (!_data.matches(reg)) {
            callbackContext.error("invalid data");
        }
        int _k = _data.length() % 2;
        if (_k != 0) {
            _data += '0';
        }
        byte[] password = Util.hexStr2Str("00000000");
        byte[] data = Util.hexStr2Str(Util.str2HexStr(_data));
        int site = message.getJSONObject(0).getInt("site");
        int addr = 0;
        if (site == 1) {
            addr = 2;
            if (length > 6 || data.length > 12) {
                callbackContext.error("data too long");
            } else {
                mZstUHFApi.writeCradTag(password, (byte) site, addr, 6, data);
            }
        } else if (site == 3) {
            length = data.length / 2;
            mZstUHFApi.writeCradTag(password, (byte) site, addr, length, data);
        }
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callbackContext.success(message);
            }
        }, 200);
    }

    private void getPower(CallbackContext callbackContext) {
        result = "";
        m_opration = STATE_GET_POWER;
        mZstUHFApi.getTransmissionPower();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                callbackContext.success(result);
            }
        }, 200);
    }

    private void setPower(JSONArray message, CallbackContext callbackContext) throws JSONException {
        int power;
        power = message.getInt(0);
        if (power > 26) {
            power = 26;
        } else if (power < 15) {
            power = 15;
        }
        mZstUHFApi.setTransmissionPower(power * 100);
        callbackContext.success(message);
    }

    private void setParam(JSONArray message, CallbackContext callbackContext) throws JSONException {
        m_opration = STATE_GET_PARAM;
        mZstUHFApi.getModemsParam();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    m_opration = STATE_SET_PARAM;
                    mThrd = message.getInt(0);
                    mZstUHFApi.setModemsParam(MixerArrValue[curMixeValue], IfampArrValue[curIfampValue], mThrd);
                    callbackContext.success("success");
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        }, 200);
    }

    private void getParam(CallbackContext callbackContext) {
        m_opration = STATE_GET_PARAM;
        mZstUHFApi.getModemsParam();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                callbackContext.success(mThrd);
            }
        }, 200);
    }

}
