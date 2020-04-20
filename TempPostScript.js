var counterP=0;
var counterC=0;
function likePost(){
//SHould read from server
counterP = request.getParameter("readCounterPNum");
counterP ++;
document.getElementById("LikeCountP").innerHTML = counterP+ " Likes.";
 }

 function likeCommon(){
//SHould read from server
 counterC = request.getParameter("readCounterCNum");
 counterC ++;
 document.getElementById("LikeCountC").innerHTML = counterC+ " Likes.";
  }
