# UHF
## cordova-plugin-UHF

> function:
>> cordova.plugins.UHF.readCard(obj, successCallBack, errorCallback);    
>> 读卡  obj = {site: int, length: int}    site:区域 1==EPC 3==USER
###
### cordova.plugins.UHF.searchCard(successCallBack, errorCallback);      
### 寻卡  success中返回所找到card的epc
### 
### cordova.plugins.UHF.writeCard(obj, successCallBack, errorCallback);   
### 写卡  obj = {data: String, site: int}   data:数据 只支持键盘上不用输入法时可输入的东西
### 
### cordova.plugins.UHF.setPower(int, successCallBack, errorCallback);    
### 设置功率 int = 功率 应为大于14并小于27的整数
### 
### cordova.plugins.UHF.getPower(successCallBack, errorCallback);         
### 获取功率 success中返回当前所感应卡的功率
### 
### cordova.plugins.UHF.getParam(successCallBack, errorCallback);        
### 获取阈值
### 
### cordova.plugins.UHF.setParam(int, successCallBack, errorCallback);   
### 设置阈值
