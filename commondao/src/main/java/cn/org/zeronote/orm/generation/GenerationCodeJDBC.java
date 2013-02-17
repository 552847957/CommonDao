/**
 * 
 */
package cn.org.zeronote.orm.generation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接数据库，生成代码
 * @author <a href='mailto:lizheng8318@gmail.com'>lizheng</a>
 *
 */
public class GenerationCodeJDBC implements IGenerationCode {

	private static Logger logger = LoggerFactory.getLogger(GenerationCodeJDBC.class);
	
	/**数据源*/
	private Connection connection;
	
	/**
	 * 
	 */
	public GenerationCodeJDBC() {
	}

	/*
	 * (non-Javadoc)
	 * @see com.dajie.framework.orm.generation.GenerationCode#generate(java.lang.String, java.lang.String)
	 */
	@Override
	public void generate(String pack, String outputFolder) {
		try {
			List<Table> tables = gainTables();
			buildSource(pack, outputFolder, tables);
		} catch (Exception e) {
			logger.error("build source error!", e);
		}
	}
	
	/**
	 * 生成代码
	 * @param pack
	 * @param outputFolder
	 * @param tables
	 * @throws IOException 
	 */
	private void buildSource(String pack, String outputFolder, List<Table> tables) throws IOException {
		String sepa = File.separator;
		if (sepa.equalsIgnoreCase("\\")) {
			sepa = "\\\\";
		}
		String baseDir = outputFolder.endsWith(sepa) ? outputFolder : outputFolder + sepa;
		
		baseDir += pack.replaceAll("\\.", sepa);
		File dir = new File(baseDir);
		if (!dir.exists()) {
			// 创建目录结构
			dir.mkdirs();
		}
		for (Table table : tables) {
			String fileName = toBeanName(table.getTableName(), true) + "PO.java";
			StringBuilder writeStr = new StringBuilder();
			writeStr.append("/**\n * \n */\n")
				.append("package ").append(pack).append(";\n\n")
				.append("import java.io.Serializable;\n")
				.append("\n")
				.append("import com.dajie.framework.orm.ORMAutoAssemble;\n")
				.append("import com.dajie.framework.orm.ORMColumn;\n")
				.append("import com.dajie.framework.orm.ORMTable;\n")
				.append("\n\n")
				.append("/**\n * Auto Generat Code by system\n *\n */\n")
				.append("@ORMAutoAssemble\n");
			// tableName
			writeStr.append("@ORMTable(tableName = \"").append(table.getTableName()).append("\")\n");
			writeStr.append("public class ").append(toBeanName(table.getTableName(), true)).append("PO").append(" implements Serializable{\n")
				.append("\n")
				.append("\tprivate static final long serialVersionUID = 1L;\n\n");
			
			// columns
			StringBuilder sgStr = new StringBuilder();	// get/set方法
			for (Column column : table.getColumns()) {
				writeStr.append("\t")
					.append("@ORMColumn(value = \"").append(column.getName()).append("\"");
				if (column.isPrimaryKey()) {
					writeStr.append(", physicalPkFld = true");
				}
				if (column.isAutoIncrement()) {
					writeStr.append(", autoIncrement = true");
				}
				if (!column.isNullable() && !column.isPrimaryKey()) {
					String val = null;
					if (column.getClz().equals(Integer.class)) {
						val = "\"0\"";
					} else if (column.getClz().equals(Long.class)) {
						val = "\"0\"";
					} else if (column.getClz().equals(Double.class)) {
						val = "\"0\"";
					} else if (column.getClz().equals(Float.class)) {
						val = "\"0\"";
					} else if (column.getClz().equals(Date.class)) {
						val = "ORMColumn.DEFAULT_DATE";
					} else {
						val = "\"\"";
					}
					writeStr.append(", defaultValue=").append(val).append("");
				}
				writeStr.append(")\n");
				writeStr.append("\t")
					.append("private ").append(column.getClz().getName()).append(" ").append(toBeanName(column.getName(), false)).append(";\n")
					.append("\n");
				
				// get
				sgStr.append("\t/**\n\t * @return the ").append(toBeanName(column.getName(), false)).append("\n\t */\n")
					.append("\t")
					.append("public ").append(column.getClz().getName()).append(" get").append(toBeanName(column.getName(), true)).append("() {\n")
					.append("\t\t")
					.append("return ").append(toBeanName(column.getName(), false)).append(";\n")
					.append("\t}\n");
				// set
				sgStr.append("\t/**\n\t * @param id the ").append(toBeanName(column.getName(), false)).append(" to set\n\t */\n")
					.append("\t")
					.append("public void set").append(toBeanName(column.getName(), true)).append("(").append(column.getClz().getName()).append(" ").append(toBeanName(column.getName(), false)).append(") {\n")
					.append("\t\t")
					.append("this.").append(toBeanName(column.getName(), false)).append(" = ").append(toBeanName(column.getName(), false)).append(";\n")
					.append("\t}\n");
			}
			// 构造方法
			writeStr.append("\t").append("/**\n\t * \n\t */\n")
				.append("\t")
				.append("public ").append(toBeanName(table.getTableName(), true)).append("PO").append("() {\n\t}\n");
			// get/set方法
			writeStr.append(sgStr);
			// 结束
			writeStr.append("\n}\n");
			File f = new File(baseDir + sepa + fileName);
			if (f.exists()) {
				f.delete();
			}
			FileWriter out = new FileWriter(f);
			out.write(writeStr.toString());
			out.flush();
			out.close();
		}
	}
	
	/**
	 * 转换名称
	 * @param dbObjName
	 * @return
	 */
	private String toBeanName(String dbObjName, boolean firstUpper) {
		// 小写
		String name = dbObjName.toLowerCase();
		// 分割"_"
		String[] strs = name.split("_");
		name = ((firstUpper ? strs[0].substring(0,1).toUpperCase() : strs[0].substring(0,1).toLowerCase()) + strs[0].substring(1));
		if (strs.length > 1) {
			for (int i = 1; i < strs.length; i++) {
				// 首字母大写
				name += (strs[i].substring(0,1).toUpperCase() + strs[i].substring(1));
			}
		}
		return name;
	}

	/**
	 * 获取所有table
	 * @return
	 * @throws SQLException 
	 */
	private List<Table> gainTables() throws SQLException {
		List<Table> tables = new ArrayList<Table>();
		try {
			ResultSet rs = null;
			// 所有表
			rs = connection.getMetaData().getTables(null, null, null, null);
			while (rs.next()) {
				Table t = new Table();
				t.setTableName(rs.getString("TABLE_NAME"));
				logger.info("tableName:{}", t.getTableName());
				tables.add(t);
			}
			rs.close();
			
			// 所有表中的列
			for (Table table : tables) {
				// 主键
				ResultSet rsp = connection.getMetaData().getPrimaryKeys(null, null, table.getTableName());
				List<String> pks = new ArrayList<String>();
				while (rsp.next()) {
					pks.add(rsp.getString("COLUMN_NAME"));
				}
				rsp.close();
				rsp = null;
				// 列
				rs = connection.getMetaData().getColumns(null, null, table.getTableName(), null);
				while (rs.next()) {
					Column c = new Column();
					c.setName(rs.getString("COLUMN_NAME"));
					if (pks.contains(c.getName())) {
						c.setPrimaryKey(true);
					}
					int dataType = rs.getInt("DATA_TYPE");
					c.setClz(toBeanType(dataType));
					int nullable = rs.getInt("NULLABLE");
					if (DatabaseMetaData.columnNullable == nullable) {
						// 明确指出可以为空
						c.setNullable(true);
					} else {
						c.setNullable(false);
					}
					c.setDef(rs.getString("COLUMN_DEF"));	// 默认值
					c.setAutoIncrement("YES".equals(rs.getString("IS_AUTOINCREMENT")));
					logger.info("tableName:{}; column:{}", table.getTableName(), c.getName());
					table.getColumns().add(c);
				}
				rs.close();
			}
			
		} catch (SQLException e) {
			throw e;
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
			}
		}
		return tables;
	}
	
	/**
	 * 转换bean需要的类型定义
	 * @param type
	 * @return
	 */
	private Class<?> toBeanType(int type) {
		Class<?> clz = typesMap.get(type);
		return clz == null ? String.class : clz;
	}
	
	/**
	 * set datasource
	 * @param dataSource the dataSource to set
	 * @throws SQLException 
	 */
	public void setDataSource(DataSource dataSource) throws SQLException {
		this.connection = dataSource.getConnection();
	}
	
	/**
	 * set xa datasource
	 * @param dataSource the XAdataSource to set
	 * @throws SQLException 
	 */
	public void setXADataSource(XADataSource dataSource) throws SQLException {
		this.connection = dataSource.getXAConnection().getConnection();
	}
	
	/**
	 * set datasource connection
	 * @param connection
	 */
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	/** 数据库类型映射，保存非String类型的对应关系，其它类型默认String */
	private static Map<Integer, Class<?>> typesMap = new HashMap<Integer, Class<?>>();
	static {
		typesMap.put(java.sql.Types.BIGINT, Long.class);
		typesMap.put(java.sql.Types.BOOLEAN, Boolean.class);
		typesMap.put(java.sql.Types.DATE, Date.class);
		typesMap.put(java.sql.Types.DECIMAL, BigDecimal.class);
		typesMap.put(java.sql.Types.DOUBLE, Double.class);
		typesMap.put(java.sql.Types.FLOAT, Float.class);
		typesMap.put(java.sql.Types.INTEGER, Integer.class);
		typesMap.put(java.sql.Types.TINYINT, Integer.class);
		typesMap.put(java.sql.Types.NUMERIC, Number.class);
		typesMap.put(java.sql.Types.TIME, Date.class);
		typesMap.put(java.sql.Types.TIMESTAMP, Date.class);
	}

}