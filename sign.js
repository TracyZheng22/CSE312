function Login(form) {
    var username = document.getElementById("n1");
    var password = document.getElementById("p1");
    if (username.value.length > 20 || username.value.length < 6) {
        alert("Username length needs to be between 6-20");
        form.reset();
        return false;
    }
    if ((username.value.charCodeAt(0) < 65)
        || (username.value.charCodeAt(0) > 90)) {
        alert("First letter needs to be capital");
        form.reset();
        return false;
    }
    if(password.value.length == 0){
        alert("Password cannot be empty!");
        form.reset();
        return false;
    }
    return true;
}