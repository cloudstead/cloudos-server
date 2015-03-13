App.RestoreRoute = Ember.Route.extend({
	model: function() {
		return {};
	},
	actions:{
		openModal: function(modalName){
			return this.render(modalName, {
				into: 'application',
				outlet: 'modal'
			});
		},
		closeModal: function(){
			this.controllerFor('restore').set("isHidden", true);
			return this.disconnectOutlet({
				outlet: 'modal',
				parentView: 'application'
			});
		}
	}
});
