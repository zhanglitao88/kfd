package com.kdf.cloud;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.kdf.cloud.base.BaseHadoop;
import com.kdf.cloud.utils.LogDecodeUtil;
import com.kdf.cloud.utils.UserAgentUtil;

public class LogSplitMrZlt extends BaseHadoop {

	/**
	 * 定义一个内部类MyMap继承Mapper类 泛型解释：LongWritable是大数据里的类型对应java中的Long类型
	 * Text对应java里的String类型，所以Mapper泛型前2个就是LongWritable, Text
	 * 逻辑解释：由于我们做的是单词计数，文件中的单词是下面2行 hello you hello me 所以 ，根据上面 步骤1.1，则 <k1,v1>是<0,
	 * hello you>,<10,hello me> 形式 文件的读取原则：<每行起始字节数，行内容>，所以第一行起始字节是0，内容是hello you
	 * 第二行起始字节是10，内容是hello me，从而得出k1,v1 步骤1.2：如果我们要实现计数，我们可以把上面的形式通过下面的map函数转换成这样
	 * <k2,v2>---> <hello,1><you,1><hello,1><me,1> 于是Mapper泛型后2个就是Text，LongWritable
	 * 可以理解泛型前2个为输入的map类型，后2个为输出的map类型
	 */
	public static class MyMap extends Mapper<LongWritable, Text, Text, Text> {
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			Text k = new Text();
			Text v = new Text();
			try {
				String resultStr = LogDecodeUtil.log2Str(value.toString());
				String[] strs = resultStr.split("&");
				Stream.of(strs).forEach(str -> {
					String[] kv = str.split("=");
					k.set(kv[0]);
					v.set(kv[1]);
//					System.err.println(k + "======" + v);
					try {
						context.write(k, v);
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * MyReduce方法的参数前两个参数是MyMap执行完后传过来的，后两个是输出参数，所以参数设置为Reducer<Text,
	 * LongWritable,Text,LongWritable> 即输入参数形式类似于： hello 1 world 1 hello 1 ers 1
	 */
	public static class MyReduce extends Reducer<Text, Text, Text, Text> {
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
//			long count = 0;
			// values就是key的所有值的集合，遍历这个集合并相加就是这个key出现的次数
			for (Text value : values) {
				if ("clientType".equals(key.toString())) {
					value = new Text(UserAgentUtil.analyticUserAgent(value.toString()).toString());
				}
				context.write(key, value);
			}
			// 将最终结果输出
//			context.write(key, new LongWritable(count));
		}
	}

	/**
	 * 判断输出路径是否存在，存在则删除，不然会报错
	 * 
	 * @param conf
	 * @param outPath
	 * @throws Exception
	 */
	public static void deleteOutPath(Configuration conf, String outPath) throws Exception {
		FileSystem fileSystem = FileSystem.get(new URI(outPath), conf);
		if (fileSystem.exists(new Path(outPath))) {
			fileSystem.delete(new Path(outPath), true);
		}
	}

	/**
	 * 将Map和Reduce操作执行
	 */
	public static void main(String[] args) throws Exception {
		String inPath = "hdfs://192.168.31.37:9000/test.log";// 文件路径
		String outDir = "hdfs://192.168.31.37:9000/test.out";// 输出目录，输出是建立一个目录，具体的内容在目录下
//		String outDir = "";
		Configuration conf = new Configuration();
		// 获取job，告诉他需要加载那个类
		Job job = Job.getInstance(conf, LogSplitMrZlt.class.getSimpleName());
		// 制定job所在的jar包，打成jar包在hadoop运行必须设置
		job.setJarByClass(LogSplitMrZlt.class);
		// 指定自己实现的Mapper类
		job.setMapperClass(MyMap.class);
		// 指定自己实现的Reducer类
		job.setReducerClass(MyReduce.class);
//		job.setNumReduceTasks(0);
		// 设置map阶段的K,V输出类型
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		// 设置最终输出的K,V输出类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		// 设置输入输出文件路径
		FileInputFormat.addInputPath(job, new Path(inPath));
		FileOutputFormat.setOutputPath(job, new Path(outDir));
		// 检查输出路径是否存在，若存在则删除
		deleteOutPath(conf, outDir);
		// 执行job，直到完成
		job.waitForCompletion(true);
		System.out.println("===============执行完成==============");

	}
}