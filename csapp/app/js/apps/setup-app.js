App = Ember.Application.create({
    // for debugging, disable in prod
    LOG_TRANSITIONS: true,
    LOG_ACTIVE_GENERATION: true
});

App.ApplicationController = Ember.ObjectController.extend({});

App.IndexRoute = Ember.Route.extend({

});

function locate(obj, path) {
    if (!path) return null;
    if (path[0] == '{' && path[path.length-1] == '}') {
        // strip leading/trailing curlies, if present
        path = path.substring(1, path.length-1);
    }
    path = path.split('.');
    var arrayPattern = /(.+)\[(\d+)\]/;
    for (var i = 0; i < path.length; i++) {
        var match = arrayPattern.exec(path[i]);
        if (match) {
            obj = obj[match[1]][parseInt(match[2])];
        } else {
            obj = obj[path[i]];
        }
    }

    return obj;
}

App.RequestMessagesObject = Ember.Object.extend({
	_doInitialization: function() {
		var self = this;
		self.set("error", this.get('json')["errors"]);
	}.on('init')
});

App.IndexController = Ember.ObjectController.extend({

    setup_key: function() {
    	try{
    		return $.url().param('key');
    	}catch(err){
    		//
    	}
    },
    accountName: '',
    initialPassword: '',
    password: '',
    password2: '',
    content: {},
    timeZones: function() {
		var time = new Date();
		var tzoffset = time.getTimezoneOffset();
		for (var i = 0; i < timeZoneList.length; i++) {
		    if (tzoffset*-1 == timeZoneList[i].ioffset) {
		    	timeZoneList[i].dfault = true;
		    	break;
		    }
		}
    	return timeZoneList;
    }.property(),
    actions: {
        doSetup: function () {
            var setupKey = this.get('setup_key');
            var name = this.get('accountName');
            var initial_password = this.get('initialPassword');
            var password = this.get('password');
            var password2 = this.get('password2');
            var error_msg = locate(Em.I18n.translations, 'errors');
            var tzone = $('#setupTZfield').find(":selected")[0].index;

            var validate = this.validateSetup(name, initial_password, password, password2);
            if ( (validate.account_name) || (validate.cs_password) || (validate.password) || (validate.confirm_password) ){
            	
				this.set('requestMessages',
						App.RequestMessagesObject.create({
							json: {"status": 'error', "api_token" : null, 
								"errors": 
									{"accountName": validate.account_name,
									"initialPassword": validate.cs_password,
									"password": validate.password,
									"password2": validate.confirm_password}}
					  })
					);
				return false;
			}

            var auth_response = Api.setup(setupKey, name, initial_password, password, tzone);
            if (auth_response) {
                CloudOs.login(auth_response);
                window.location.replace('/admin.html');
            } else {
                alert('error, perhaps the key was not correct. check your email again.');
            }
        }
    },
    validateSetup: function(account_name, cs_password, password, confirm_password) {
    	var response = {"account_name":null, "cs_password":null, "password":null, "confirm_password":null};
    	var error_msg = locate(Em.I18n.translations, 'errors');
    	var pattern = /^[a-z][a-z0-9]+$/i;
    	
    	if ((account_name.trim() == '') || (!account_name)){
			response.account_name = error_msg.field_required;
		}else if(!pattern.test(account_name)){
			response.account_name = error_msg.account_name_invalid;
		}
    	
    	if ((cs_password.trim() == '') || (!cs_password)){
			response.cs_password = error_msg.field_required;
		}
    	
    	if ((password.trim() == '') || (!password)){
			response.password = error_msg.field_required;
		}else if(password.length < 8) {
			response.password = error_msg.password_short;
		}
    	
    	if ((confirm_password.trim() == '') || (!confirm_password)){
			response.confirm_password = error_msg.field_required;
		}
    	
    	if (password != confirm_password) {
			response.confirm_password = error_msg.password_mismatch;
		}
    	
    	return response;
    }
});

