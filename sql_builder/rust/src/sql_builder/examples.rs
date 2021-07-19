use crate::kalai;
pub fn f_1() -> String {
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp_1: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp_1.insert(
            String::from(":from"),
            kalai::Value::MVector(
                {
                    let mut tmp_2: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_2.push(kalai::Value::String(String::from(":foo")));
                    tmp_2
                }
                .clone(),
            ),
        );
        tmp_1.insert(
            String::from(":select"),
            kalai::Value::MVector(
                {
                    let mut tmp_3: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_3.push(kalai::Value::String(String::from(":a")));
                    tmp_3.push(kalai::Value::String(String::from(":b")));
                    tmp_3.push(kalai::Value::String(String::from(":c")));
                    tmp_3
                }
                .clone(),
            ),
        );
        tmp_1.insert(
            String::from(":where"),
            kalai::Value::MVector(
                {
                    let mut tmp_4: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_4.push(kalai::Value::String(String::from(":=")));
                    tmp_4.push(kalai::Value::String(String::from(":f.a")));
                    tmp_4.push(kalai::Value::String(String::from("baz")));
                    tmp_4
                }
                .clone(),
            ),
        );
        tmp_1
    };
    return crate::sql_builder::core::format(query_map);
}
