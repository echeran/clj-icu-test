#[macro_use]
extern crate lazy_static;
use std::collections::HashMap;
use std::collections::HashSet;
use std::vec::Vec;
use std::env;
fn main () {
let args: Vec<String> = env::args().collect();
{
println!("{}", std::env::var(String::from("USER")).unwrap());
}
}