var net = require('net');

var error = function (data) {
  console.log('Error ' + data);
};

var lambda = function (message) {
  return {message: message};
};

var server = net.createServer(function(socket){
  socket.on('data', function (data) {
    var parsedData, message, result;
    console.log('Recieved ' + data);
    try{
      parsedData = JSON.parse(data.toString());
      message = parsedData.message;
      result = lambda(message);
    }catch(e){
      console.log('Failed to parse ' + data);
      result = {error: "Failed to parse message as JSON", message: data.toString()};
    }

    client = net.connect({host: "default.docker", port: 5065}, function () {
      console.log('Connected to Logstash');
      var jsonResult = JSON.stringify(result);
      client.write(jsonResult);
      client.write("\n");
      client.end();
    });
    client.on('error', error);
  });
  socket.on('error', error);
});

server.listen({host: "0.0.0.0", port: 5055}, function () {
  console.log('Listening on 5055');
});
