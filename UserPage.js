let socket = new WebSocket('ws://' + window.location.host + '/websocket');
socket.binaryType = "arraybuffer";
socket.onopen = function() {
    console.log("Connected.")
};

socket.onmessage = function(e) {
    console.log(evt.msg);
};

socket.onclose = function() {
    console.log("Connection is closed...");
};

//Ryan note: since we are sending different messages over the same websocket
//Remember to send a unique ID first for two way identification!
//Perhaps something like username + sequence number + randomized integer to
//roughly handle upload concurrency.
function sendMessage() {
    var type = 0;
    var message = document.getElementById("formmsg").value;
    var id = document.getElementById("NameOfUser").innerHTML;
    var file = document.getElementById("formmedia");
    var filename = file.value.split(/(\\|\/)/g).pop();
    console.log(message.length  + " " + file.files.length);
    if(message.length != 0 && file.files.length==1)
    {
        type = 2;
        var reader = new FileReader();

        reader.onload = function(e)
        {
            rawData = e.target.result;
        };
        reader.readAsArrayBuffer(file.files[0]);
    }else if(message.length != 0){
        console.log("send: " + type + id.length + id + message);
        var buf = new Uint8Array(message.length+ 2 + id.length);
        buf[0] = type;
        buf[1] = id.length;
        for(let i=0; i<id.length; i++){
            buf[i+2] = id.charCodeAt(i);
        }
        for(let i=0; i<message.length; i++){
            buf[i+2+id.length] = message.charCodeAt(i);
        }
        socket.send(buf);
    }else if(file.files.length==1)
    {
        type = 1;
        console.log("send: " + type);
        var reader = new FileReader();

        reader.onload = function(e)
        {
            rawData = new Uint8Array(e.target.result);                                                                                    
            console.log(rawData);
            buf = new Uint8Array(rawData.length + 3 + id.length + filename.length);
            buf[0] = type;
            buf[1] = id.length;
            for(let i=0; i<id.length; i++){
                buf[i+2] = id.charCodeAt(i);
            }
            counter = 2+id.length;
            buf[counter] = filename.length;
            counter++;
            for(let i=0; i<filename.length; i++){
                buf[i+counter] = filename.charCodeAt(i);
            }
            counter += filename.length;
            for(let i=0; i<rawData.length; i++){
                buf[i+counter] = rawData[i];
            }
            socket.send(buf);
        };

        reader.readAsArrayBuffer(file.files[0]);
    }
    document.getElementById("cform").reset();
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
