import com.fasterxml.jackson.databind.JsonNode;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConfigManager {

    private static ConfigManager thisManager;
    private static Config thisCurrConfig;

    private ConfigManager(){

    }

    public static ConfigManager getInstance(){
        if (thisManager==null){
            thisManager = new ConfigManager();
        }
        return thisManager;
    }

    public void loadConfigFile(String filePath) {
        FileReader fileReader = null;
        try{
            fileReader = new FileReader(filePath);
            System.out.println("File path is valid!");
        }
        catch(FileNotFoundException e){
            throw new HttpConfigException(e);
        }
            StringBuffer b = new StringBuffer();
            int i;

            try {
                while ((i = fileReader.read()) != -1) {
                    b.append((char) i);

                }
            }
            catch(IOException e){
                throw new HttpConfigException((e));
            }
            try {
                JsonNode config = Json.parse(b.toString());
                thisCurrConfig = Json.fromJson(config, Config.class);
            }
            catch(IOException e){
                throw new HttpConfigException("Error parsing configuration");
            }
    }

    public Config getCurrentConfig(){
        if( thisCurrConfig == null){
            throw new HttpConfigException("No configuration set");
        }
        return thisCurrConfig;
    }
}
