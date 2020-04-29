let socket = new WebSocket('ws://' + window.location.host + '/websocket');
socket.binaryType = "arraybuffer";
socket.onopen = function() {
    console.log("Connected.");
    sendRequest(4, 0);
};

function sendRequest(type, n){
    console.log("Initial Request");
    var id = document.getElementById("NameOfUser").innerHTML;
    var buf = new Uint8Array(2 + id.length + 1);
    buf[0] = type;
    buf[1] = id.length;
    for(let i=0; i<id.length; i++){
        buf[i+2] = id.charCodeAt(i);
    }
    buf[2+id+length+1]=n;
    socket.send(buf);
}

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
    if(message.length != 0 && file.files.length>=1)
    {
        alert("Please send either a message or a file. (Not Both)");
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
    }else if(file.files.length==1){
        type = 1;
        console.log("send: " + type);
        var reader = new FileReader();
        
        /*var filesize = ((file.files[0].size/1024)/1024).toFixed(4);
        
        if(filesize > 15){
            alert("No files greater than 15 MB!");
            document.getElementById("cform").reset();
            return;
        }*/
        
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

socket.onmessage = renderMessages;

function renderMessages(message) {
    var data = new Uint8Array(message.data);
    console.log(data);
    var type = data[0];
    console.log("Received type " + type);
    
    if(type == 0){
        console.log("Message Post Received!");
        var id_length = data[1];
        var id_bin = new Uint8Array(id_length);
        for(let i=0; i<id_length; i++){
            id_bin[i] = data[i+2];
        }
        var id =  new TextDecoder("utf-8").decode(id_bin);
        var msg = new TextDecoder("utf-8").decode(data.subarray(id_length+2, data.length));
        console.log(msg);

        //https://developer.mozilla.org/en-US/docs/Web/HTML/Element/template
        var template = document.querySelector('#posts');
        var tbody = document.querySelector("#serverPosts");
        var clone = template.content.cloneNode(true);
        clone.querySelector(".smallName").textContent = id;
        clone.querySelector(".postMessage").textContent = msg;
        
        tbody.appendChild(clone);
    }else if(type == 1){
        console.log("File Post Received!");
        var id_length = data[1];
        var id_bin = new Uint8Array(id_length);
        for(let i=0; i<id_length; i++){
            id_bin[i] = data[i+2];
        }
        var id =  new TextDecoder("utf-8").decode(id_bin);
        var file_len = data[id_length+2];
        console.log("file_len " + file_len);
        var file_bin = new Uint8Array(file_len);
        for(let i=0; i<file_len; i++){
            file_bin[i] = data[i+3+id_length];
        }
        var filename = new TextDecoder("utf-8").decode(file_bin);
        localStorage.setItem(filename, data.subarray(file_len+id_length+3, data.length));
        console.log("Saved: " + filename);
        var dataImage = localStorage.getItem(filename);
        
        var template = document.querySelector('#multiposts');
        var tbody = document.querySelector("#serverPosts");
        var clone = template.content.cloneNode(true);
        clone.querySelector(".smallName").textContent = id;
        
        if(filename.substr(filename.lastIndexOf('.'), filename.length).includes(".jpg")){
            clone.querySelector(".mediaContent").innerHTML = "<img src=data:image/jpg;" + btoa(dataImage) + ">";
        }
        
        tbody.appendChild(clone);
    }
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
