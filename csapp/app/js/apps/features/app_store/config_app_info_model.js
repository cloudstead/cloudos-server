App.ConfigAppInfo = Ember.Object.extend({});

App.ConfigAppInfo.reopenClass({
	all: Ember.ArrayProxy.create({content: []}),

	createNew: function(obj) {
		var newObj = App.ConfigAppInfo.create(obj);
		App.ConfigAppInfo.all.pushObject(newObj);
		return newObj;
	}
});
