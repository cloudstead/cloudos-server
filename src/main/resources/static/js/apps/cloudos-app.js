App = Ember.Application.create({
    LOG_TRANSITIONS: true // for debugging, disable in prod
});


App.ApplicationRoute = Ember.Route.extend({
    model: function() {
        return {
            cloudos_session: sessionStorage.getItem('cloudos_session'),
            cloudos_account: CloudOs.account()
        };
    },
    setupController: function(controller, model) {

        // is HTML5 storage even supported?
        if (typeof(Storage) == "undefined") {
            alert('Your browser is not supported. Please use Firefox, Chrome, Safari 4+, or IE8+');
            return;
        }

        // do we have an API token?
        if (!model.cloudos_session) {
            this.transitionTo('login');
            return;
        }

        // is the token valid?
        var account = Api.account_for_token(model.cloudos_session);
        if (!account) {
            CloudOs.logout();
            this.transitionTo('login');
            return;
        }

        CloudOs.set_account(account);

//        this.transitionTo('app', 'files');
//        this.transitionTo('app', AppRuntime.app_model('email'));
    }
});

App.IndexRoute = App.ApplicationRoute;

App.Router.map(function() {
    this.resource('login');
    this.resource('logout');
    this.resource('settings');
    this.resource('app', { path: '/app/:app_name' });
});

App.LogoutRoute = Ember.Route.extend({
    setupController: function(controller, model) {
        CloudOs.logout();
        window.location.replace('/index.html');
        return;
    }
});

App.ApplicationController = Ember.ObjectController.extend({
    cloudos_session: sessionStorage.getItem('cloudos_session'),
    cloudos_account: CloudOs.account(),
    actions: {
        'select_app': function (app_name) {
            this.transitionTo('app', App.app_model(app_name));
        }
    }
});

function get_username () {
    const account = CloudOs.account();
    return account ? account.name : null;
}

App.IndexController = Ember.ObjectController.extend({
    cloudos_account: CloudOs.account(),
    username: get_username()
});

App.LoginController = Ember.ObjectController.extend({
    cloudos_account: CloudOs.account(),
    username: get_username(),
    password: '',
    actions: {
        doLogin: function () {
            var creds = {
                'name': this.get('username'),
                'password': this.get('password')
            };
            var auth_response = Api.login_account(creds);
            if (auth_response && auth_response.sessionId) {
                CloudOs.login(auth_response);
                window.location.replace('/index.html');
            } else {
                // populate error
            }
        }
    }
});

App.SettingsRoute = Ember.Route.extend({
    model: function () {
        return {
            "current_password": "",
            "new_password": "",
            "new_password2": ""
        }
    }
});

App.SettingsController = Ember.ObjectController.extend({
    actions: {
        changePassword: function () {
            var newPassword = this.get('new_password');
            var confirmPassword = this.get('new_password2');
            if (newPassword != confirmPassword) {
                msg_alert('password_mismatch');
                return;
            }
            if (Api.change_password(CloudOs.account().name, this.get('current_password'), newPassword)) {
                this.transitionTo('index');
            } else {
                console.log('error changing password');
            }
        }
    }
});

App.app_model = function (app_name) {
    var app_url = "/api/app/load/"+app_name;
    return { "app_name": app_name,
             "app_url":  app_url + "?" + Api.API_TOKEN + "=" + sessionStorage.getItem('cloudos_session') };
};

App.AppRoute = Ember.Route.extend({
    model: function(params) {
        return App.app_model(params.app_name);
    }
});

Ember.Handlebars.helper('app-name', function(name) {
    var appName = Em.I18n.translations['appNames'][name];
    if (!appName) return '??undefined translation: appNames.'+name;
    return appName;
});
