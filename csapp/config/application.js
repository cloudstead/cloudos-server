module.exports = function(lineman) {
	//Override application configuration here. Common examples follow in the comments.
	return {

		removeTasks:{
			common: ["jshint","handlebars"],
		},
		appendTasks:{
			dist: ["copy:local","copy:csapp_gen","copy:csapp_dist","copy:build"],
		},
		copy: {
			local: {
				files:[
						{	expand: true,
							cwd: 'app/js/local/',
							src: '**/*',
							dest: 'dist/js/local',
							}
				]	
			},
			csapp_dist: {
				files:[
						{	expand: true,
							cwd: 'app/js/apps/',
							src: '*app*',
							dest: 'dist/js/apps',
							}
				]	
			},
			csapp_gen: {
				files:[
						{	expand: true,
							cwd: 'app/js/apps/',
							src: '*app*',
							dest: 'generated/js/apps',
							}
				]	
			},
			build: {
				files:[
						{	expand: true,
							cwd: 'dist',
							src: '**/*',
							dest: '../target/classes/static',
							}
				]	
			}
		}

	};
};
