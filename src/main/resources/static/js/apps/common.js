String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

CloudOs = {
    json_safe_parse: function (j) {
        return j ? JSON.parse(j) : null;
    },

    login: function (auth_response) {
        sessionStorage.setItem('cloudos_session', auth_response.sessionId);
        CloudOs.set_account(auth_response.account);
    },

    logout: function () {
        sessionStorage.clear();
    },

    account: function () {
        return CloudOs.json_safe_parse(sessionStorage.getItem('cloudos_account'));
    },

    set_account: function (account) {
        sessionStorage.setItem('cloudos_account', JSON.stringify(account));
    }

};

