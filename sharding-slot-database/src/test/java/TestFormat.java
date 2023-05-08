import com.rlynic.sharding.slot.database.strategy.RewriteEngineInterceptor;

public class TestFormat {

    public static void main(String[] args) {
        String sql = "FROM pan_file WHERE (file_id) in ( ( ),( ),(?) )";
        System.out.println(RewriteEngineInterceptor.formatString(sql));
        String sql2 = ", version_num FROM pan_file WHERE (file_id) in ( (?),(?),( ) )";
        System.out.println(RewriteEngineInterceptor.formatString(sql2));
        String sql3 = ", version_num FROM pan_file WHERE (file_id) in ( (?),(   ),(?),( ) )";
        System.out.println(RewriteEngineInterceptor.formatString(sql3));
        String sql4 = ", version_num FROM pan_file WHERE (file_id) in ( (?)   ,(   )  ,(?),( ) )";
        System.out.println(RewriteEngineInterceptor.formatString(sql4));
        String sql5 = ", version_num FROM pan_file WHERE (file_id) in ( ( \n  )  ,(?),(?),( ) )";
        System.out.println(RewriteEngineInterceptor.formatString(sql5));
        String sql6 = ", version_num FROM pan_file WHERE (file_id) in ( ( 3  )  ,  (?),(   )  ,(?),( ) )";
        System.out.println(RewriteEngineInterceptor.formatString(sql6));

    }
}
