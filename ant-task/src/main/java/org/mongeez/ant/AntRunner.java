package org.mongeez.ant;

import java.net.UnknownHostException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.mongeez.Mongeez;
import org.springframework.core.io.ClassPathResource;

import com.mongodb.Mongo;

public class AntRunner extends Task {
    private boolean executeEnabled = false;
    private String dbName;
	private String host;
	private Integer port;
	private String filePath;

    // The method executing the task
    public void execute() throws BuildException {
    	Mongeez mongeez = new Mongeez();
    	mongeez.setFile(new ClassPathResource(filePath));
    	try {
            mongeez.setMongo(new Mongo(host, port));
        } catch (UnknownHostException e) {
            throw new BuildException(e);
        }
    	mongeez.setDbName(dbName);
    	mongeez.process();
    }

	public void setExecuteEnabled(boolean executeEnabled) {
		this.executeEnabled = executeEnabled;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
}