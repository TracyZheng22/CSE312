let socket = new WebSocket('ws://' + window.location.host + '/websocket');
ws.binaryType = "arraybuffer";
ws.onopen = function() {
    console.log("Connected.")
};

ws.onmessage = function(e) {
    console.log(evt.msg);
};

ws.onclose = function() {
    console.log("Connection is closed...");
};

//Ryan note: since we are sending different messages over the same websocket
//Remember to send a unique ID first for two way identification!
//Perhaps something like username + sequence number + randomized integer to
//roughly handle upload concurrency.
function sendMessage() {
    var type = 0;
    var message = document.getElementById("formmsg").value;
    var file = document.getElementById("formmedia");
    if(message.byteLength != 0 && file.files.length)
    {
        type = 2;
        var reader = new FileReader();

        reader.onload = function(e)
        {
            rawData = e.target.result;
        };
        reader.readAsBinaryString(file.files[0]);
    }else if(message.byteLength != 0){
        console.log("send: " + "0" + document.getElementById("NameOfUser").innerHTML+message);
        socket.send("0"+document.getElementById("NameOfUser").innerHTML +message);
    }else if(file.files.length)
    {
        type = 1;
        var reader = new FileReader();

        reader.onload = function(e)
        {
            rawData = e.target.result;
        };

        reader.readAsBinaryString(file.files[0]);
    }
    var reset = document.getElementById("formmsg");
    reset.value = reset.defaultValue;
    socket.send(document.getElementById("formmedia"));
 //This part might be use for show image on the page, but still yet sure to put it or not
 //socket.onmessage = function (event) { 
 //var bytes = new Uint8Array(event.data);
 //   var data = "";
 //   var len = bytes.byteLength;
 //   for (var i = 0; i < len; ++i) {
 //   	data += String.fromCharCode(bytes[i]);
 //   }
 //};

}

var coll = document.getElementsByClassName("collapse");
for (var i = 0; i < coll.length; i++) {
  coll[i].addEventListener("click", function() {
    this.classList.toggle("active");
    var postDiv = this.nextElementSibling;
    if (postDiv.style.display === "block") {
      postDiv.style.display = "none";
    } else {
      postDiv.style.display = "block";
    }
  });
}

var open = false;
var fl = document.getElementById("friendslist");
function friendsList() {
    if (!open) {
        fl.style.bottom = "0";
        open = true;
    } else {
        fl.style.bottom = "-330px";
        open = false;
    }
}

/* THROWS ERROR: FIX
$('.nav ul li').click(function(){
  $(this).addClass("active").siblings().removeClass('active');
})

const TabButton = document.querySelectorAll('.nav ul li');
const Tab = document.querySelectorAll('tab');

function changeTabs(panelIndex){
  Tab.forEach((function(node) {
    node.style.display = 'none';
  });
  Tab[panelIndex].style.display = 'block';
}
Tab(0);
*/
