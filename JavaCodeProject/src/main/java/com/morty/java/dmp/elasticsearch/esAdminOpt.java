package com.morty.java.dmp.elasticsearch;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.translog.Translog;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.morty.java.dmp.elasticsearch.EsAdminOpt.refreshType.ALL;

/**
 * Created by Administrator on 2016/05/13.
 *
 * ES Administration Operation
 */
public class EsAdminOpt {
    public Client client;
    public  AdminClient adminClient;
    public IndicesAdminClient indicesAdminClient;
    public ClusterAdminClient clusterAdminClient;
    public Settings settings;          //ES Settings
    public Properties properties;
    public GetSettingsResponse getSettingsResponse;
    public ClusterHealthResponse clusterHealthResponse;

    // TODO INIT OPERATION
    public void init(){
        properties.put("index.number_of_shareds",3);
        properties.put("index_number_of_replicas",2);

    }

    /**
     * AdminClient adminClient = client.admin();
     * @return
     */
    public AdminClient  getAdminClient(){
        try{
            adminClient= client.admin();
            return adminClient;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * IndicesAdminClient indicesAdminClient = client.admin().indices();
     * @return
     */
    public IndicesAdminClient getIndicesAdminClient(){
        try{
            indicesAdminClient=  client.admin().indices();
            return indicesAdminClient;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * clusterAdminClient clusterAdminClient=  client.admin().cluster();
     * @return
     */
    public ClusterAdminClient getClusterAdminClient(){
        try{
            clusterAdminClient=  client.admin().cluster();
            return clusterAdminClient;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据设置创建索引和type
     * @param indexName     索引名称
     * @param properties    索引属性设置
     */
    public void createIndex(String indexName,String typeName,Properties properties){
        try{
            indicesAdminClient.prepareCreate(indexName).setSettings(Settings.builder().put(properties)).
                    addMapping(typeName)
                    .get();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 对已有的index创建type或更新现有的type
     * @param indexName
     * @param typeName
     * @param source
     */
    public void addAndUpdateIndexType(String indexName, String typeName, Translog.Source source){
        try{
            indicesAdminClient.preparePutMapping(indexName).setType(typeName).setSource(source).get();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 刷新ES
     *@param Type
     * @param indexs
     */
    public void  refreshIndex(refreshType Type,String ... indexs){
        try {
            if(Type == ALL){
                indicesAdminClient.prepareRefresh().get();
            }else {
                indicesAdminClient.prepareRefresh(indexs).get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取ES设置
     * @param indexs
     * @return
     */
    public Object getIndexSetting(String ... indexs){
        Map<String,Object> settingMap= new HashMap<String, Object>();
        try {
            getSettingsResponse=indicesAdminClient.prepareGetSettings(indexs).get();
            for(ObjectObjectCursor<String,Settings> cursor:getSettingsResponse.getIndexToSettings()){
                String index=cursor.key;
                Settings settings=cursor.value;
                Integer shards=settings.getAsInt("index.number_of_shards",null);
                Integer replicas=settings.getAsInt("index.number_of_replicas",null);
                settingMap.put("index",index);
                settingMap.put("shards",shards);
                settingMap.put("replicas",replicas);
                settingMap.put("getSetting",true);
             }
        } catch (Exception e) {
            e.printStackTrace();
            settingMap.put("getSetting",false);
        }
        return settingMap;
    }

    //-----------------------------------Cluster Admin-----------------------------------------
    public Object getClusterInfo(String ...indexs){
        Map<String,Object> cluserMap=new HashMap<String, Object>();
        try {
            clusterHealthResponse=clusterAdminClient.prepareHealth(indexs).get();    //集群信息
            String clusterName=clusterHealthResponse.getClusterName();  //集群名称
            int numberofDataNodes=clusterHealthResponse.getNumberOfDataNodes();
            int numberofNodes=clusterHealthResponse.getNumberOfNodes();
            cluserMap.put("clusterName",clusterName);
            cluserMap.put("numberofDataNodes",numberofDataNodes);
            cluserMap.put("numberofNodes",numberofNodes);
            //TODO 循环获取多个index的信息
         /*   for(ClusterIndexHealth health : clusterHealthResponse){
                String index=health.getIndex();
                int numberOfShards = health.getNumberOfShards();
                int numberOfReplicas = health.getNumberOfReplicas();
                ClusterHealthStatus status = health.getStatus();
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }

        return  cluserMap;
    }

    /**
     *
     * @param indexs
     */
    public  void waitStatus(String...indexs){
        ClusterHealthResponse clusterHealthResponse=clusterAdminClient.prepareHealth(indexs).
                setWaitForGreenStatus().
                setTimeout(TimeValue.timeValueSeconds(2))
                .get();
        ClusterHealthStatus clusterHealthStatus=clusterHealthResponse.
                getIndices().
                get(indexs).
                getStatus();
        if(!clusterHealthStatus.equals(ClusterHealthStatus.GREEN)){
            throw new RuntimeException("Index is in"+clusterHealthStatus+" status ");
        }
    }

    public  enum  refreshType {ONE, ALL, MANY}    //刷新类型


}
