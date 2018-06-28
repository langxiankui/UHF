var exec = require('cordova/exec');

var coolMethod = function () {};

coolMethod.readCard = function (arg0, success, error) {
    exec(success, error, 'UHF', 'readCard', [arg0]);
}

coolMethod.searchCard = function (success, error, arg0) {
    exec(success, error, 'UHF', 'searchCard', [arg0]);
}

coolMethod.writeCard = function (arg0, success, error) {
    exec(success, error, 'UHF', 'writeCard', [arg0]);
}

coolMethod.getPower = function (success, error, arg0) {
    exec(success, error, 'UHF', 'getPower', [arg0]);
}

coolMethod.setPower = function (arg0, success, error) {
    exec(success, error, 'UHF', 'setPower', [arg0]);
}

coolMethod.getParam = function (success, error, arg0) {
    exec(success, error, 'UHF', 'getParam', [arg0]);
}

coolMethod.setParam = function (arg0, success, error) {
    exec(success, error, 'UHF', 'setParam', [arg0]);
}

coolMethod.readTid = function (success, error, arg0) {
    exec(success, error, 'UHF', 'readTid', [arg0]);   
}

module.exports = coolMethod;
