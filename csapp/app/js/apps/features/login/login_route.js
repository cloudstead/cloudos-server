App.LoginRoute = Ember.Route.extend({
	beforeModel: function(transition) {
		this._resetLoginControllerMessages();
	},

	_resetLoginControllerMessages: function() {
		var loginController = this.controllerFor('login');
		loginController.set('notificationForgotPassword', null);
		loginController.set('requestMessages', null);
	},
	actions: {
		didTransition: function() {
			if(!Ember.isNone(CloudOs.account())){
				this.transitionTo('index');
			}
		}
	}

});
