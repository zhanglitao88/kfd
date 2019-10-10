package com.kdf.etl.job;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kdf.etl.base.BaseHadoop;
import com.kdf.etl.constant.Constants;
import com.kdf.etl.hadoop.MyMapper;
import com.kdf.etl.service.HbaseService;
import com.kdf.etl.service.HdfsService;
import com.kdf.etl.service.HiveJdbcService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EtlJob  extends BaseHadoop{
	
	@Autowired
	private HdfsService hdfsService;
	@Autowired
	private HbaseService hbaseService;
	
	@Autowired
	private HiveJdbcService hiveJdbcService;

	static String dfs = "yyyy_MM_dd_HH";
	static String dfs_year = "yyyy-MM-dd";
	static String dfs_hour = "HH";
	
	String sqlTemplet = "CREATE EXTERNAL TABLE ^HIVETABLENAME^ \r\n" + 
			"(key string,appid string, user_agent string, method string,ip string,port string,url string,request_time string)  \r\n" + 
			"STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'  \r\n" + 
			"WITH SERDEPROPERTIES (\"hbase.columns.mapping\" = \":key,log:appid,log:user_agent,log:method,log:ip,log:port,log:url,log:request_time\")  \r\n" + 
			"TBLPROPERTIES (\"hbase.table.name\" = \"^HBASETABLENAME^\")";
	
	@Scheduled(fixedRate = 100000)
	public void etl() throws Exception {
		log.info("===============数据清洗执行开始==============\"");
		String nowDate = DateTimeFormatter.ofPattern(dfs).format(LocalDateTime.now().minusHours(1));
		String hbaseTableName = Constants.TABLENAME + nowDate;
		String hiveTableName = Constants.TABLENAME +"hive_"+ nowDate;
		
		
		String year = DateTimeFormatter.ofPattern(dfs_year).format(LocalDateTime.now());
		
		String hour = DateTimeFormatter.ofPattern(dfs_hour).format(LocalDateTime.now());

		String hdfsFilePath = year + "/"+hour+"/";

		// 创建hbase&hive表
		hbaseService.createTable(hbaseTableName, Constants.COLF);
		String createHiveSql = sqlTemplet.replace("^HIVETABLENAME^", hiveTableName);
		createHiveSql = createHiveSql.replace("^HBASETABLENAME^", hbaseTableName);
		hiveJdbcService.createTable(createHiveSql);
		
		//hdfs 目录下所有文件
//		List<Map<String, String>> list = hdfsService.listFile(Constants.HDFS+"/"+hdfsFilePath);
		List<Map<String, String>> list = hdfsService.listFile(Constants.HDFS+"/"+"flume/nginx/20191010/09/");
		Configuration conf = hbaseService.getConf();
		Job job = Job.getInstance(conf, EtlJob.class.getName());
		job.setJarByClass(EtlJob.class);
		job.setMapperClass(MyMapper.class);
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(Put.class);
		TableMapReduceUtil.initTableReducerJob(hbaseTableName, null, job, null, null, null, null, false);
		job.setNumReduceTasks(0);
//		job.setReducerClass(MyReduce.class);
		for(Map<String, String> map :list) {
			FileInputFormat.addInputPath(job,new Path(map.get("filePath")));
		}
		
		job.waitForCompletion(true);
		log.info("===============数据清洗执行完成==============");

	}

	public static void main(String[] args) {

		String nowDate = DateTimeFormatter.ofPattern(dfs).format(LocalDateTime.now().minusHours(1));
		String hbaseTableName = Constants.TABLENAME + nowDate;
		
		String year = DateTimeFormatter.ofPattern(dfs_year).format(LocalDateTime.now());
		
		String hour = DateTimeFormatter.ofPattern(dfs_hour).format(LocalDateTime.now());
		System.out.println(nowDate);
		System.out.println(hbaseTableName);
		System.out.println(year);
		System.out.println(hour);
	}


}
