App.ConfigAppView = Ember.View.extend({
	didInsertElement: function() {
		this.get('controller').send("checkForConfig");
	}
});
