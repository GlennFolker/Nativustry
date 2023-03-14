#![feature(let_chains)]
#![feature(once_cell)]

use jni::{
    JNIEnv,
    errors::Error,
    objects::{
        GlobalRef,
        JClass, JStaticMethodID,
        JValue,
    },
    signature::{
        ReturnType, Primitive,
    },
};

use std::sync::OnceLock;

struct JniData {
    class_log: GlobalRef,
    method_log_info: JStaticMethodID,
}

static DATA: OnceLock<JniData> = OnceLock::new();

#[no_mangle]
#[allow(unused_must_use)]
pub extern "C" fn Java_nativustry_Nativustry_initialize(mut env: JNIEnv, _: JClass) {
    if let Ok(data) = (|| {
        let class_log = env.find_class("arc/util/Log")?;
        let method_log_info = env.get_static_method_id(&class_log, "info", "(Ljava/lang/String;[Ljava/lang/Object;)V")?;

        Ok::<JniData, Error>(JniData {
            class_log: env.new_global_ref(class_log)?,
            method_log_info,
        })
    })() && DATA.set(data).is_err() {
        env.throw_new("java/lang/IllegalStateException", "initialize() may not be called twice").unwrap();
    }
}

#[no_mangle]
#[allow(unused_must_use)]
pub extern "C" fn Java_nativustry_Nativustry_sayHello(mut env: JNIEnv, _: JClass) {
    (|| {
        let Some(data) = DATA.get() else {
            return env.throw_new("java/lang/RuntimeException", "Native data not initialized; call initialize() first");
        };

        let message = env.new_string("Hello everyone, this is Nativustry!")?;
        unsafe {
            env.call_static_method_unchecked(
                &data.class_log,
                &data.method_log_info,
                ReturnType::Primitive(Primitive::Void),
                &[
                    JValue::Object(&message).as_jni(),
                ],
            )?;
        }

        Ok::<(), Error>(())
    })();
}
