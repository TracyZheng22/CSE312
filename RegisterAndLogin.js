function Register() {
    var username = document.getElementById("RegisterUserName");
    var password = document.getElementById("RegisterPassword");
    var error = document.getElementById("error");
    var hasLetter = false;
    var hasNumber = false;
    if (username.value.length > 20 || username.value.length < 6) {
        error.innerHTML = "Username length needs to be between 6-20";
        return false;
    }
    if ((username.value.charCodeAt(0) < 65)
        || (username.value.charCodeAt(0) > 90)) {
        error.innerHTML = "First letter needs to be capital";
        return false;
    }
    if (password.value.length > 20 || password.value.length < 6) {
        error.innerHTML = "Password length needs to be between 6-20";
        return false;
    }
    for (var i = 0; i < password.value.length; i++) {
        if ((password.value.charCodeAt(i) >= 48)
            && (password.value.charCodeAt(i) <= 57)) {
            hasNumber = true;
        }
        if ((password.value.charCodeAt(i) >= 65)
            && (password.value.charCodeAt(i) <= 122)) {
            hasLetter = true;
        }
    }
    if (hasLetter && hasNumber) {
        return true;
    } else {
        error.innerHTML = "Password needs to be a combination of letters and numbers";
        return false;
    }
}