var Ruuvitag = require("Ruuvitag");
Ruuvitag.setAccelOn(true);
Ruuvitag.setEnvOn(true);

setInterval(function () {
  var battery = `${NRF.getBattery().toFixed(2)}`;
  var temp = `${Ruuvitag.getEnvData().temp.toFixed(1)}`;
  var pres = `${Ruuvitag.getEnvData().pressure.toFixed(1)}`;
  var humid = `${Ruuvitag.getEnvData().humidity.toFixed(1)}`;
  var json = `${battery},${temp},${pres},${humid}`;
  NRF.nfcURL(json);
}, 5000);


setInterval(function () {
  var accX = Math.round(Ruuvitag.getAccelData().x);
  var accZ = Math.round(Ruuvitag.getAccelData().z);
  var data = `${accX},${accZ}`;

  NRF.updateServices({
    "6e400001-b5a3-f393-e0a9-e50e24dcca9e": {
      "6e400003-b5a3-f393-e0a9-e50e24dcca9e": {
       value : [data],
        notify: true
      }
    }
  });
}, 1000);


