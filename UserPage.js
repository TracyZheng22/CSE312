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