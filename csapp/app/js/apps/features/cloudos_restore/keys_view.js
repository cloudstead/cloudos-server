App.KeysView = Ember.View.extend({

	didInsertElement: function(event) {
		Ember.run.next(function(){
			$('textarea').focus();
			$('textarea').select();
		});
	}
});
