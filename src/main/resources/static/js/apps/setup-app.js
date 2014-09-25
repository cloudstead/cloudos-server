App = Ember.Application.create({
    // for debugging, disable in prod
    LOG_TRANSITIONS: true,
    LOG_ACTIVE_GENERATION: true
});

App.ApplicationController = Ember.ObjectController.extend({});

App.IndexRoute = Ember.Route.extend({

});

App.IndexController = Ember.ObjectController.extend({

    setup_key: $.url().param('key'),
    accountName: '',
    initialPassword: '',
    password: '',
    password2: '',

    actions: {
        doSetup: function () {
            var setupKey = this.get('setup_key');
            var name = this.get('accountName');
            var initial_password = this.get('initialPassword');
            var password = this.get('password');
            if (this.get('password2') != password) {
                alert('passwords do not match'); // todo: i18n this
            }
            var auth_response = Api.setup(setupKey, name, initial_password, password);
            if (auth_response) {
                CloudOs.login(auth_response);
                window.location.replace('/admin.html');
            } else {
                alert('error, perhaps the key was not correct. check your email again.');
            }
        }
    }
});