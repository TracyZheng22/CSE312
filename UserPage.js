let socket = new WebSocket('ws://' + window.location.host + '/websocket');
socket.binaryType = "arraybuffer";
socket.onopen = function() {
    console.log("Connected.");
    sendRequest(4, 0, null);
};

function sendRequest(type, n, objid){
    console.log("Request " + type);
    var id = document.getElementById("NameOfUser").innerHTML;
    var buf = new Uint8Array(2 + id.length + 1 + 12);
    buf[0] = type;
    buf[1] = id.length;
    for(let i=0; i<id.length; i++){
        buf[i+2] = id.charCodeAt(i);
    }
    if(objid!=null){
        for(let i=0; i<12; i++){
            buf[i+2+id.length] = objid[i];
        }
    }
    buf[2+id+length+13]=n;
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
    var message = document.getElementById("fmsg").value;
    var id = document.getElementById("NameOfUser").innerHTML;
    var file = document.getElementById("fmedia");
    var filename = file.value.split(/(\\|\/)/g).pop();
    console.log(message.length  + " " + file.files.length);
    if(message.length != 0 && file.files.length>=1)
    {
        alert("Please send either a message or a file. (Not Both)");
    }else if(message.length != 0){
        console.log("send: " + type + id.length + id + message);
        var buf = new Uint8Array(message.length+ 2 + id.length + 13);
        buf[0] = type;
        buf[1] = id.length;
        for(let i=0; i<id.length; i++){
            buf[i+2] = id.charCodeAt(i);
        }
        buf[2+id.length+12] = 0;
        for(let i=0; i<message.length; i++){
            buf[i+2+id.length+13] = message.charCodeAt(i);
        }
        socket.send(buf);
    }else if(file.files.length==1){
        type = 1;
        console.log("send: " + type);
        var reader = new FileReader();
        
        var filesize = ((file.files[0].size/1024)/1024).toFixed(4);
        
        if(filesize > 1){
            alert("No files greater than 1 MB! (This is to preserve space)");
            document.getElementById("cform").reset();
            return;
        }
        
        reader.onload = function(e)
        {
            rawData = new Uint8Array(e.target.result);                                                                                    
            console.log(rawData);
            buf = new Uint8Array(rawData.length + 3 + id.length + filename.length + 13);
            buf[0] = type;
            buf[1] = id.length;
            for(let i=0; i<id.length; i++){
                buf[i+2] = id.charCodeAt(i);
            }
            counter = 2+id.length+12;
            buf[counter] = 0;
            counter++;
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

function sendComment(curr){
    var _idcom = curr.id;
    var type = 3;
    var objstr = _idcom.substr(0, _idcom.length-3);
    //Note: change all ids in phase 3 to actual username
    var id = document.getElementById("NameOfUser").innerHTML;
    var message = curr.getElementsByClassName("formmsg")[0].value;
    console.log("Send " + type + " " + message);
    
    var buf = new Uint8Array(message.length+ 2 + id.length + 12 + 12 + 1);
    buf[0] = type;
    buf[1] = id.length;
    var counter = 2;
    for(let i=0; i<id.length; i++){
        buf[i+counter] = id.charCodeAt(i);
    }
    counter+=id.length;
    for (var i = 0; i < 12; i++) {
        buf[i+counter] = objstr.charCodeAt(i);
    }
    counter+=12;
    buf[counter] = 0;
    counter++;
    counter+=12;
    for(let i=0; i<message.length; i++){
        buf[i+counter] = message.charCodeAt(i);
    }
    socket.send(buf);
    curr.reset();
}

socket.onmessage = renderMessages;

function renderMessages(message) {
    var data = new Uint8Array(message.data);
    console.log(data);
    var type = data[0];
    console.log("Received type " + type);
    
    var id_length = data[1];
    var counter = 2;
    var id_bin = new Uint8Array(id_length);
    for(let i=0; i<id_length; i++){
        id_bin[i] = data[i+counter];
    }
    counter+=id_length;
    var objid = new Uint8Array(12);
    for(let i=0; i<12; i++){
        objid[i] = data[i+counter];
    }
    var objstr = '';
    for (var i = 0; i < objid.length; i++) {
        objstr += String.fromCharCode(objid[i]);
    }
    counter+=12;
    var likes = data[counter];
    counter++;
    var id =  new TextDecoder("utf-8").decode(id_bin);
    
    if(type == 0){
        console.log("Message Post Received!");
        var msg = new TextDecoder("utf-8").decode(data.subarray(counter, data.length));
        console.log(msg);

        //https://developer.mozilla.org/en-US/docs/Web/HTML/Element/template
        var template = document.querySelector('#posts');
        var tbody = document.querySelector("#serverPosts");
        var clone = template.content.cloneNode(true);
        clone.querySelector(".smallName").textContent = id;
        clone.querySelector(".postMessage").textContent = msg;
        clone.querySelector(".reactions").textContent = likes;
        clone.querySelector(".reactions").setAttribute("id", objstr+"likes");
        clone.querySelector(".likeButton").setAttribute("id", objstr);
        clone.querySelector(".comform").setAttribute("id", objstr+"com");
        clone.querySelector(".commentholder").setAttribute("id", objstr+"comholder");
        
        tbody.appendChild(clone);
    }else if(type == 1){
        console.log("File Post Received!");
        var file_len = data[counter];
        counter++;
        console.log("file_len " + file_len);
        var file_bin = new Uint8Array(file_len);
        for(let i=0; i<file_len; i++){
            file_bin[i] = data[i+counter];
        }
        counter+=file_len;
        var filename = new TextDecoder("utf-8").decode(file_bin);
        var dataFile = data.subarray(counter, data.length);
        
        var template = document.querySelector('#multiposts');
        var tbody = document.querySelector("#serverPosts");
        var clone = template.content.cloneNode(true);
        clone.querySelector(".smallName").textContent = id;
        
        var filext = filename.substr(filename.lastIndexOf('.'), filename.length);
        var datastr = '';
        for (var i = 0; i < dataFile.length; i++) {
            datastr += String.fromCharCode(dataFile[i]);
        }
        var mime = extToMimes[filext];
        console.log("type: " + mime);
        
        if(mime.includes("image/")){
            clone.querySelector(".mediaContent").innerHTML = "<img src=data:"+mime+";base64," + btoa(datastr) + ">";
        }else{
            clone.querySelector(".mediaContent").innerHTML = "<iframe width='100%' height='100%' src=data:"+mime+";base64," + btoa(datastr) + ">" + filename + "</iframe>";
        }
        clone.querySelector(".likeButton").setAttribute("id", objstr);
        clone.querySelector(".reactions").setAttribute("id", objstr+"likes");
        clone.querySelector(".commentholder").setAttribute("id", objstr+"comholder");
        clone.querySelector(".comform").setAttribute("id", objstr+"com");
        clone.querySelector(".reactions").textContent = likes;
        
        tbody.appendChild(clone);
    }else if(type==2){
        console.log("Like Received!");
        var post = document.getElementById(objstr+"likes");      
        if(post!=null){
            post.textContent = likes;
        }
    }else if(type==3){
        console.log("Comment Received!");
        var post = document.getElementById(objstr+"comholder");
        console.log(objstr+"comholder");
        if(post!=null){
            //This means that it exists, so we need to use the comment template to add one
            
            //Get correct objstr
            var objid = new Uint8Array(12);
            for(let i=0; i<12; i++){
                objid[i] = data[i+counter];
            }
            var objstr = '';
            for (var i = 0; i < objid.length; i++) {
                objstr += String.fromCharCode(objid[i]);
            }
            counter+=12;
            
            //Get message
            var msg = new TextDecoder("utf-8").decode(data.subarray(counter, data.length));
            console.log(msg);
            
            var template = document.querySelector('#comments');
            var tbody = post;
            var clone = template.content.cloneNode(true);
            clone.querySelector(".smallName").textContent = id;
            clone.querySelector(".comment").textContent = msg;
            clone.querySelector(".reactions").textContent = likes;
            clone.querySelector(".reactions").setAttribute("id", objstr+"likes");
            clone.querySelector(".likeButton").setAttribute("id", objstr);
            tbody.appendChild(clone);
        }
    }
}

function like(objstr){
    var objid = new Uint8Array(12);
    for (var i = 0; i < 12; i++) {
        objid[i] = objstr.charCodeAt(i);
    }
    console.log("Like objid: " + objid);
    sendRequest(2, 1, objid);
}
                    
function collapse(col) {
    col.classList.toggle("active");
    var postDiv = col.nextElementSibling;
    if (postDiv.style.display === "block") {
      postDiv.style.display = "none";
    } else {
      postDiv.style.display = "block";
    }
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

var extToMimes = {
    '.jpeg': 'image/jpeg',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.xml': 'text/xml',
    '.gif': 'image/gif',
    '.txt': 'text/plain',
    '.doc': 'application/msword',
    '.pdf': 'application/pdf',
    '.xls': 'application/vnd.ms-excel',
    '.ppt': 'application/vnd.ms-powerpoint',
    '.zip': 'application/zip',
    '.mid': 'audio/midi',
    '.midi': 'audio/midi',
    '.kar': 'audio/midi',
    '.mp3': 'audio/mpeg',
    '.3gpp': 'video/3gpp',
    '.3gp': 'video/3gpp',
    '.mpg': 'video/mpeg',
    '.mpeg': 'video/mpeg',
    '.mov': 'video/quicktime',
    '.flv': 'video/x-flv',
    '.mng': 'video/x-mng',
    '.wmv': 'video/x-ms-wmv',
    '.avi': 'video/x-msvideo',
    '.m4v': 'video/mp4',
    '.mp4': 'video/mp4'
};



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
