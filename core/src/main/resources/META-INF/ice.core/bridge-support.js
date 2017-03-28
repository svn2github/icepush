window.ice=window.ice?window.ice:{};
window.ice.lib={};
ice.module=function module(_1){
var _2={};
function _3(_4,_5){
if(_2[_4]){
throw "variable \""+_4+"\" already defined";
}
_2[_4]=_5;
return _5;
};
_1(_3);
return _2;
};
ice.importFrom=function importFrom(_6){
var _7=window;
var _8=_6.split(".");
for(var i=0,_9=_8.length;i<_9;i++){
_7=_7[_8[i]];
}
var _a=[];
for(var p in _7){
if(_7.hasOwnProperty(p)){
_a.push("var "+p+"="+_6+"[\""+p+"\"]");
}
}
return _a.join(";");
};
ice.evaluate=eval;
ice.lib.oo=ice.module(function(_b){
function _c(a){
return a&&!!a.push;
};
function _d(s){
return typeof s=="string";
};
function _e(s){
return typeof s=="number";
};
function _f(s){
return typeof s=="boolean";
};
function _10(s){
return typeof s.length=="number";
};
function _11(o){
return o.instanceTag==o;
};
var uid=(function(){
var id=0;
return function(){
return id++;
};
})();
function _12(){
throw "operation not supported";
};
function _13(_14){
return function(){
var _15=arguments;
var _16=arguments[0];
if(_16.instanceTag&&_16.instanceTag==_16){
var _17=_16(arguments.callee);
if(_17){
return _17.apply(_17,_15);
}else{
_12();
}
}else{
return _14?_14.apply(_14,_15):_12();
}
};
};
var _18=_13(String);
var _19=_13(Number);
var _1a=_13(function(o){
var s;
if(_d(o)){
s=o;
}else{
if(_e(o)){
return Math.abs(Math.round(o));
}else{
s=o.toString();
}
}
var h=0;
for(var i=0,l=s.length;i<l;i++){
var c=parseInt(s[i],36);
if(!isNaN(c)){
h=c+(h<<6)+(h<<16)-h;
}
}
return Math.abs(h);
});
var _1b=_13(function(a,b){
return a==b;
});
function _1c(_1d){
var _1e=[];
var _1f=[];
var _20=null;
var id=uid();
_1e.push(_1a);
_1f.push(function(_21){
return id;
});
_1e.push(_1b);
_1f.push(function(_22,_23){
return _22==_23;
});
_1e.push(_18);
_1f.push(function(_24){
return "Object:"+id.toString(16);
});
_1d(function(_25,_26){
var _27=_1e.length;
for(var i=0;i<_27;i++){
if(_1e[i]==_25){
_1f[i]=_26;
return;
}
}
_1e.push(_25);
_1f.push(_26);
},function(_28){
_20=_28;
});
function _29(_2a){
var _2b=_1e.length;
for(var i=0;i<_2b;i++){
if(_1e[i]==_2a){
return _1f[i];
}
}
return _20;
};
return _29.instanceTag=_29;
};
function _2c(){
var _2d=arguments[0];
var _2e=arguments;
var o=_1c(_2d);
function _2f(_30){
var _31=o(_30);
if(_31){
return _31;
}else{
var _32=_2e.length;
for(var i=1;i<_32;i++){
var _33=_2e[i];
var _34=_33(_30);
if(_34){
return _34;
}
}
return null;
}
};
return _2f.instanceTag=_2f;
};
_b("isArray",_c);
_b("isString",_d);
_b("isNumber",_e);
_b("isBoolean",_f);
_b("isIndexed",_10);
_b("isObject",_11);
_b("asString",_18);
_b("asNumber",_19);
_b("hash",_1a);
_b("equal",_1b);
_b("operationNotSupported",_12);
_b("operator",_13);
_b("object",_1c);
_b("objectWithAncestors",_2c);
});
ice.lib.functional=ice.module(function(_35){
function _36(fun,_37){
return fun.apply(fun,_37);
};
function _38(){
var _39=arguments;
return function(fun){
_36(fun,_39);
};
};
function _3a(){
var _3b=arguments;
return function(){
var _3c=[];
var fun=_3b[0];
for(var i=1;i<_3b.length;i++){
_3c.push(_3b[i]);
}
for(var j=0;j<arguments.length;j++){
_3c.push(arguments[j]);
}
return _36(fun,_3c);
};
};
function _3d(_3e,_3f){
return function(val){
var _40=arguments;
var _41=[];
var _42=[];
_3e(function(_43,run){
_41.push(_43);
_42.push(run);
});
var _44=_41.length;
for(var i=0;i<_44;i++){
if(_36(_41[i],_40)){
return _36(_42[i],_40);
}
}
if(_3f){
_36(_3f,_40);
}
};
};
function _45(arg){
return arg;
};
function _46(b){
return !b;
};
function _47(a,b){
return a>b;
};
function _48(a,b){
return a<b;
};
function not(a){
return !a;
};
function _49(a,b){
return a*b;
};
function _4a(a,b){
return a+b;
};
function max(a,b){
return a>b?a:b;
};
function _4b(_4c,_4d){
return _4c+(_4d?_4d:1);
};
function _4e(_4f,_50){
return _4f-(_50?_50:1);
};
function any(){
return true;
};
function _51(){
return false;
};
function _52(){
};
_35("apply",_36);
_35("withArguments",_38);
_35("curry",_3a);
_35("$witch",_3d);
_35("identity",_45);
_35("negate",_46);
_35("greater",_47);
_35("less",_48);
_35("not",not);
_35("multiply",_49);
_35("plus",_4a);
_35("max",max);
_35("increment",_4b);
_35("decrement",_4e);
_35("any",any);
_35("none",_51);
_35("noop",_52);
});
ice.lib.delay=ice.module(function(_53){
eval(ice.importFrom("ice.lib.oo"));
var run=operator();
var _54=operator();
var _55=operator();
function _56(f,_57){
return object(function(_58){
var id=null;
var _59=false;
_58(run,function(_5a,_5b){
if(id||_59){
return;
}
var _5c=_5b?function(){
try{
f();
}
finally{
if(--_5b<1){
_55(_5a);
}
}
}:f;
id=setInterval(_5c,_57);
return _5a;
});
_58(_54,function(_5d){
return run(_5d,1);
});
_58(_55,function(_5e){
if(id){
clearInterval(id);
id=null;
}else{
_59=true;
}
});
});
};
_53("run",run);
_53("runOnce",_54);
_53("stop",_55);
_53("Delay",_56);
});
ice.lib.string=ice.module(function(_5f){
function _60(s,_61){
var _62=s.indexOf(_61);
if(_62>=0){
return _62;
}else{
throw "\""+s+"\" does not contain \""+_61+"\"";
}
};
function _63(s,_64){
var _65=s.lastIndexOf(_64);
if(_65>=0){
return _65;
}else{
throw "string \""+s+"\" does not contain \""+_64+"\"";
}
};
function _66(s,_67){
return s.indexOf(_67)==0;
};
function _68(s,_69){
var _6a=s.lastIndexOf(_69);
return _6a>-1&&(_6a==s.length-_69.length);
};
function _6b(s,_6c){
return s.indexOf(_6c)>=0;
};
function _6d(s){
return /^\s*$/.test(s);
};
function _6e(s,_6f){
return s.length==0?[]:s.split(_6f);
};
function _70(s,_71,_72){
return s.replace(_71,_72);
};
function _73(s){
return s.toLowerCase();
};
function _74(s){
return s.toUpperCase();
};
function _75(s,_76,to){
return s.substring(_76,to);
};
function _77(s){
s=s.replace(/^\s+/,"");
for(var i=s.length-1;i>=0;i--){
if(/\S/.test(s.charAt(i))){
s=s.substring(0,i+1);
break;
}
}
return s;
};
var _78=Number;
function _79(s){
return "true"==s||"any"==s;
};
function _7a(s){
return new RegExp(s);
};
_5f("indexOf",_60);
_5f("lastIndexOf",_63);
_5f("startsWith",_66);
_5f("endsWith",_68);
_5f("containsSubstring",_6b);
_5f("blank",_6d);
_5f("split",_6e);
_5f("replace",_70);
_5f("toLowerCase",_73);
_5f("toUpperCase",_74);
_5f("substring",_75);
_5f("trim",_77);
_5f("asNumber",_78);
_5f("asBoolean",_79);
_5f("asRegexp",_7a);
});
ice.lib.collection=ice.module(function(_7b){
eval(ice.importFrom("ice.lib.functional"));
eval(ice.importFrom("ice.lib.oo"));
var _7c=operator($witch(function(_7d){
_7d(isString,function(_7e,_7f){
return _7e.indexOf(_7f);
});
_7d(isArray,function(_80,_81){
for(var i=0,_82=_80.length;i<_82;i++){
if(_80[i]==_81){
return i;
}
}
return -1;
});
_7d(any,operationNotSupported);
}));
var _83=operator(function(_84,_85){
return _84.concat(_85);
});
var _86=operator(function(_87,_88){
if(isArray(_87)){
_87.push(_88);
return _87;
}else{
operationNotSupported();
}
});
var _89=operator($witch(function(_8a){
_8a(isArray,function(_8b,_8c){
_8b.unshift(_8c);
return _8b;
});
_8a(any,operationNotSupported);
}));
var _8d=operator(function(_8e,_8f){
var _90=_8e.length;
for(var i=0;i<_90;i++){
_8f(_8e[i],i);
}
});
var _91=operator(function(_92,_93,_94){
var _95=_93;
var _96=_92.length;
for(var i=0;i<_96;i++){
_95=_94(_95,_92[i]);
}
return _95;
});
var _97=operator($witch(function(_98){
_98(isArray,function(_99,_9a){
return _91(_99,[],function(_9b,_9c){
return _9a(_9c)?_86(_9b,_9c):_9b;
});
});
_98(isString,function(_9d,_9e){
return _91(_9d,"",function(_9f,_a0){
return _9e(_a0)?_83(_9f,_a0):_9f;
});
});
_98(isIndexed,function(_a1,_a2){
return _f0(function(_a3){
function _a4(_a5,end){
if(_a5>end){
return null;
}
var _a6=_a1[_a5];
return _a2(_a6)?function(){
return _a3(_a6,_a4(_a5+1,end));
}:_a4(_a5+1,end);
};
return _a4(0,_a1.length-1);
});
});
}));
var _a7=operator(function(_a8,_a9,_aa){
var _ab=_a8.length;
for(var i=0;i<_ab;i++){
var _ac=_a8[i];
if(_a9(_ac,i)){
return _ac;
}
}
return _aa?_aa(_a8):null;
});
var _ad=operator($witch(function(_ae){
_ae(isString,function(_af,_b0){
return _af.indexOf(_b0)>-1;
});
_ae(isArray,function(_b1,_b2){
var _b3=_b1.length;
for(var i=0;i<_b3;i++){
if(equal(_b1[i],_b2)){
return true;
}
}
return false;
});
_ae(any,operationNotSupported);
}));
var _b4=operator(function(_b5){
return _b5.length;
});
var _b6=operator(function(_b7){
_b7.length=0;
});
var _b8=operator(function(_b9){
return _b9.length==0;
});
var _ba=function(_bb){
return !_b8(_bb);
};
var _bc=operator($witch(function(_bd){
_bd(isString,function(_be,_bf){
return _91(_be,"",function(_c0,_c1){
return _83(_c0,_bf(_c1));
});
});
_bd(isArray,function(_c2,_c3){
return _91(_c2,[],function(_c4,_c5){
return _86(_c4,_c3(_c5));
});
});
_bd(isIndexed,function(_c6,_c7){
return _f0(function(_c8){
function _c9(_ca,end){
if(_ca>end){
return null;
}
return function(){
return _c8(_c7(_c6[_ca],_ca),_c9(_ca+1,end));
};
};
return _c9(0,_c6.length-1);
});
});
}));
var _cb=operator(function(_cc,_cd){
return _ce(_cc).sort(function(a,b){
return _cd(a,b)?-1:1;
});
});
var _cf=operator(function(_d0){
return _ce(_d0).reverse();
});
var _ce=operator(function(_d1){
return _91(_d1,[],curry(_86));
});
var _d2=operator(function(_d3,_d4){
return _d3.join(_d4);
});
var _d5=operator();
var _d6=function(_d7,_d8){
return _97(_d7,function(i){
return !_d8(i);
});
};
var _d9=operator(function(_da,_db){
return _97(_da,curry(_ad,_db));
});
var _dc=operator(function(_dd,_de){
return _d6(_dd,curry(_ad,_de));
});
var _df=function(_e0,_e1){
_e1=_e1||[];
_8d(_e0,function(i){
apply(i,_e1);
});
};
var _e2=function(_e3){
return function(){
var _e4=arguments;
_8d(_e3,function(i){
apply(i,_e4);
});
};
};
var _e5=function(_e6){
return _91(_e6,[],_86);
};
var _e7=function(_e8){
return _91(_e8,[],function(set,_e9){
if(not(_ad(set,_e9))){
_86(set,_e9);
}
return set;
});
};
var key=operator();
var _ea=operator();
function _eb(k,v){
return object(function(_ec){
_ec(key,function(_ed){
return k;
});
_ec(_ea,function(_ee){
return v;
});
_ec(asString,function(_ef){
return "Cell["+asString(k)+": "+asString(v)+"]";
});
});
};
function _f0(_f1){
var _f2=_f1(_eb);
return object(function(_f3){
_f3(_8d,function(_f4,_f5){
var _f6=_f2;
while(_f6!=null){
var _f7=_f6();
_f5(key(_f7));
_f6=_ea(_f7);
}
});
_f3(_91,function(_f8,_f9,_fa){
var _fb=_f9;
var _fc=_f2;
while(_fc!=null){
var _fd=_fc();
_fb=_fa(_fb,key(_fd));
_fc=_ea(_fd);
}
return _fb;
});
_f3(_d2,function(_fe,_ff){
var _100;
var _101=_f2;
while(_101!=null){
var cell=_101();
var _102=asString(key(cell));
_100=_100?_100+_ff+_102:_102;
_101=_ea(cell);
}
return _100;
});
_f3(_bc,function(self,_103){
return _f0(function(_104){
function _105(_106){
if(!_106){
return null;
}
var cell=_106();
return function(){
return _104(_103(key(cell)),_105(_ea(cell)));
};
};
return _105(_f2);
});
});
_f3(_ad,function(self,item){
var _107=_f2;
while(_107!=null){
var cell=_107();
if(item==key(cell)){
return true;
}
_107=_ea(cell);
}
return false;
});
_f3(_b4,function(self){
var _108=_f2;
var i=0;
while(_108!=null){
i++;
_108=_ea(_108());
}
return i;
});
_f3(_97,function(self,_109){
return _f0(function(_10a){
function _97(_10b){
if(!_10b){
return null;
}
var cell=_10b();
var k=key(cell);
var v=_ea(cell);
return _109(k)?function(){
return _10a(k,_97(v));
}:_97(v);
};
return _97(_f2);
});
});
_f3(_a7,function(self,_10c,_10d){
var _10e=_f2;
var _10f;
while(_10e!=null){
var cell=_10e();
var k=key(cell);
if(_10c(k)){
_10f=k;
break;
}
_10e=_ea(cell);
}
if(_10f){
return _10f;
}else{
return _10d?_10d(self):null;
}
});
_f3(_b8,function(self){
return _f2==null;
});
_f3(_ce,function(self){
return _f0(_f1);
});
_f3(asString,function(self){
return "Stream["+_d2(self,", ")+"]";
});
});
};
_7b("indexOf",_7c);
_7b("concatenate",_83);
_7b("append",_86);
_7b("insert",_89);
_7b("each",_8d);
_7b("inject",_91);
_7b("select",_97);
_7b("detect",_a7);
_7b("contains",_ad);
_7b("size",_b4);
_7b("empty",_b6);
_7b("isEmpty",_b8);
_7b("notEmpty",_ba);
_7b("collect",_bc);
_7b("sort",_cb);
_7b("reverse",_cf);
_7b("copy",_ce);
_7b("join",_d2);
_7b("inspect",_d5);
_7b("reject",_d6);
_7b("intersect",_d9);
_7b("complement",_dc);
_7b("broadcast",_df);
_7b("broadcaster",_e2);
_7b("asArray",_e5);
_7b("asSet",_e7);
_7b("key",key);
_7b("value",_ea);
_7b("Cell",_eb);
_7b("Stream",_f0);
});
ice.lib.configuration=ice.module(function(_110){
eval(ice.importFrom("ice.lib.oo"));
eval(ice.importFrom("ice.lib.string"));
eval(ice.importFrom("ice.lib.collection"));
var _111=operator();
var _112=operator();
var _113=operator();
var _114=operator();
var _115=operator();
var _116=operator();
var _117=operator();
function _118(_119){
function _11a(s){
return "true"==toLowerCase(s);
};
function _11b(name){
var a=_119().getAttribute(name);
if(a){
return a;
}else{
throw "unknown attribute: "+name;
}
};
function _11c(name){
return collect(asArray(_119().getElementsByTagName(name)),function(e){
var _11d=e.firstChild;
return _11d?_11d.nodeValue:"";
});
};
return object(function(_11e){
_11e(_111,function(self,name,_11f){
try{
return _11b(name);
}
catch(e){
if(isString(_11f)){
return _11f;
}else{
throw e;
}
}
});
_11e(_113,function(self,name,_120){
try{
return Number(_11b(name));
}
catch(e){
if(isNumber(_120)){
return _120;
}else{
throw e;
}
}
});
_11e(_112,function(self,name,_121){
try{
return _11a(_11b(name));
}
catch(e){
if(isBoolean(_121)){
return _121;
}else{
throw e;
}
}
});
_11e(_117,function(self,name){
var _122=_119().getElementsByTagName(name);
if(isEmpty(_122)){
throw "unknown configuration: "+name;
}else{
return _118(function(){
return _119().getElementsByTagName(name)[0];
});
}
});
_11e(_114,function(self,name,_123){
var _124=_11c(name);
return isEmpty(_124)&&_123?_123:_124;
});
_11e(_116,function(self,name,_125){
var _126=_11c(name);
return isEmpty(_126)&&_125?_125:collect(_126,Number);
});
_11e(_115,function(self,name,_127){
var _128=_11c(name);
return isEmpty(_128)&&_127?_127:collect(_128,_11a);
});
});
};
_110("attributeAsString",_111);
_110("attributeAsBoolean",_112);
_110("attributeAsNumber",_113);
_110("valueAsStrings",_114);
_110("valueAsBooleans",_115);
_110("valueAsNumbers",_116);
_110("childConfiguration",_117);
_110("XMLDynamicConfiguration",_118);
});
ice.lib.window=ice.module(function(_129){
eval(ice.importFrom("ice.lib.functional"));
eval(ice.importFrom("ice.lib.collection"));
function _12a(_12b,obj,_12c){
if(obj.addEventListener){
obj.addEventListener(_12b,_12c,false);
return function(){
obj.removeEventListener(_12b,_12c,false);
};
}else{
var type="on"+_12b;
obj.attachEvent(type,_12c);
return function(){
obj.detachEvent(type,_12c);
};
}
};
var _12d=curry(_12a,"load");
var _12e=curry(_12a,"unload");
var _12f=curry(_12a,"beforeunload");
var _130=curry(_12a,"resize");
var _131=curry(_12a,"keypress");
var _132=curry(_12a,"keyup");
window.width=function(){
return window.innerWidth?window.innerWidth:(document.documentElement&&document.documentElement.clientWidth)?document.documentElement.clientWidth:document.body.clientWidth;
};
window.height=function(){
return window.innerHeight?window.innerHeight:(document.documentElement&&document.documentElement.clientHeight)?document.documentElement.clientHeight:document.body.clientHeight;
};
_129("registerListener",_12a);
_129("onLoad",_12d);
_129("onUnload",_12e);
_129("onBeforeUnload",_12f);
_129("onResize",_130);
_129("onKeyPress",_131);
_129("onKeyUp",_132);
});
ice.lib.cookie=ice.module(function(_133){
eval(ice.importFrom("ice.lib.oo"));
eval(ice.importFrom("ice.lib.string"));
eval(ice.importFrom("ice.lib.collection"));
function _134(name){
var _135=detect(split(asString(document.cookie),"; "),function(_136){
return startsWith(_136,name);
},function(){
throw "Cannot find value for cookie: "+name;
});
return decodeURIComponent(contains(_135,"=")?split(_135,"=")[1]:"");
};
function _137(name,_138){
try{
return _139(name,_134(name));
}
catch(e){
if(_138){
return _138();
}else{
throw e;
}
}
};
function _13a(name){
var _13b=true;
_137(name,function(){
_13b=false;
});
return _13b;
};
var _13c=operator();
var _13d=operator();
function _139(name,val,path){
val=val||"";
path=path||"/";
document.cookie=name+"="+encodeURIComponent(val)+"; path="+path;
return object(function(_13e){
_13e(value,function(self){
return _134(name);
});
_13e(_13c,function(self,val){
document.cookie=name+"="+encodeURIComponent(val)+"; path="+path;
return self;
});
_13e(_13d,function(self){
var date=new Date();
date.setTime(date.getTime()-24*60*60*1000);
document.cookie=name+"=; expires="+date.toGMTString()+"; path="+path;
});
_13e(asString,function(self){
return "Cookie["+name+", "+value(self)+", "+path+"]";
});
});
};
_133("lookupCookieValue",_134);
_133("lookupCookie",_137);
_133("existsCookie",_13a);
_133("update",_13c);
_133("remove",_13d);
_133("Cookie",_139);
});
ice.lib.query=ice.module(function(_13f){
eval(ice.importFrom("ice.lib.functional"));
eval(ice.importFrom("ice.lib.oo"));
eval(ice.importFrom("ice.lib.collection"));
var _140=operator();
var _141=operator();
function _142(name,_143){
return objectWithAncestors(function(_144){
_144(_140,function(self){
return encodeURIComponent(name)+"="+encodeURIComponent(_143);
});
_144(_141,function(self,_145){
_146(_145,self);
});
},Cell(name,_143));
};
var _146=operator();
var _147=operator();
var _148=operator();
var _149=operator();
var _14a=operator();
function _14b(){
var _14c=[];
return object(function(_14d){
_14d(_148,function(self){
return _14c;
});
_14d(_146,function(self,_14e){
append(_14c,_14e);
return self;
});
_14d(_147,function(self,name,_14f){
append(_14c,_142(name,_14f));
return self;
});
_14d(_149,function(self,_150){
_141(_150,self);
return self;
});
_14d(_141,function(self,_151){
each(_14c,curry(_146,_151));
});
_14d(_140,function(self){
return join(collect(_14c,_140),"&");
});
_14d(_14a,function(self,uri){
if(not(isEmpty(_14c))){
return uri+(contains(uri,"?")?"&":"?")+_140(self);
}else{
return uri;
}
});
_14d(asString,function(self){
return inject(_14c,"",function(_152,p){
return _152+"|"+key(p)+"="+value(p)+"|\n";
});
});
});
};
_13f("asURIEncodedString",_140);
_13f("serializeOn",_141);
_13f("Parameter",_142);
_13f("Query",_14b);
_13f("addParameter",_146);
_13f("addNameValue",_147);
_13f("queryParameters",_148);
_13f("addQuery",_149);
_13f("appendToURI",_14a);
});
ice.lib.http=ice.module(function(_153){
eval(ice.importFrom("ice.lib.functional"));
eval(ice.importFrom("ice.lib.oo"));
eval(ice.importFrom("ice.lib.collection"));
eval(ice.importFrom("ice.lib.query"));
var _154=operator();
var _155=operator();
var _156=operator();
var _157=operator();
var _158=operator();
var _159=_153("Client",function(_15a){
var _15b;
if(window.XMLHttpRequest){
_15b=function(){
return new XMLHttpRequest();
};
}else{
if(window.ActiveXObject){
_15b=function(){
return new window.ActiveXObject("Microsoft.XMLHTTP");
};
}else{
throw "cannot create XMLHttpRequest";
}
}
function _15c(_15d){
var _15e=Query();
_15d(_15e);
return _15e;
};
var _15f=_15a?_181:noop;
return object(function(_160){
_160(_155,function(self,uri,_161,_162,_163){
var _164=_15b();
var _165=_185(_164);
var _166=_190(_164);
_164.open("GET",appendToURI(_15c(_161),uri),true);
_162(_165);
_164.onreadystatechange=function(){
if(_164.readyState==4){
_163(_166,_165);
_15f(_165);
}
};
_164.send("");
return _165;
});
_160(_154,function(self,uri,_167,_168,_169){
var _16a=_15b();
var _16b=_185(_16a);
var _16c=_190(_16a);
_16a.open("GET",appendToURI(_15c(_167),uri),false);
_168(_16b);
_16a.send("");
_169(_16c,_16b);
_15f(_16b);
});
_160(_157,function(self,uri,_16d,_16e,_16f){
var _170=_15b();
var _171=_185(_170);
var _172=_190(_170);
_170.open("POST",uri,true);
_16e(_171);
_170.onreadystatechange=function(){
if(_170.readyState==4){
_16f(_172,_171);
_15f(_171);
}
};
var _173=typeof _16d=="function"?_15c(_16d):_16d;
_170.send(_173);
return _171;
});
_160(_156,function(self,uri,_174,_175,_176){
var _177=_15b();
var _178=_185(_177);
var _179=_190(_177);
_177.open("POST",uri,false);
_175(_178);
var _17a=typeof _174=="function"?_15c(_174):_174;
_177.send(_17a);
_176(_179,_178);
_15f(_178);
});
_160(_158,function(self,uri,_17b,_17c,_17d){
var _17e=_15b();
var _17f=_185(_17e);
var _180=_190(_17e);
_17e.open("DELETE",appendToURI(_15c(_17b),uri),true);
_17c(_17f);
_17e.onreadystatechange=function(){
if(_17e.readyState==4){
_17d(_180,_17f);
_15f(_17f);
}
};
_17e.send("");
return _17f;
});
});
});
var _181=operator();
var _182=operator();
var _183=operator();
var _184=operator();
function _185(_186){
return object(function(_187){
_187(_183,function(self,name,_188){
_186.setRequestHeader(name,_188);
});
_187(_181,function(self){
_186.onreadystatechange=noop;
});
_187(_182,function(self){
_186.onreadystatechange=noop;
_186.abort();
_187(_182,noop);
});
});
};
var _189=operator();
var _18a=operator();
var _18b=operator();
var _18c=operator();
var _18d=operator();
var _18e=operator();
var _18f=operator();
function _190(_191){
return object(function(_192){
_192(_189,function(){
try{
return _191.status;
}
catch(e){
return 0;
}
});
_192(_18a,function(self){
try{
return _191.statusText;
}
catch(e){
return "";
}
});
_192(_18d,function(self,name){
try{
var _193=_191.getResponseHeader(name);
return _193&&_193!="";
}
catch(e){
return false;
}
});
_192(_18b,function(self,name){
try{
return _191.getResponseHeader(name);
}
catch(e){
return null;
}
});
_192(_18c,function(self,name){
try{
return collect(reject(split(_191.getAllResponseHeaders(),"\n"),isEmpty),function(pair){
var _194=split(pair,": ");
return Cell(_194[0],_194[1]);
});
}
catch(e){
return [];
}
});
_192(_18e,function(self){
try{
return _191.responseText;
}
catch(e){
return "";
}
});
_192(_18f,function(self){
try{
return _191.responseXML;
}
catch(e){
var txt="<error>"+e+"</error>";
var doc;
if(window.DOMParser){
var _195=new DOMParser();
doc=_195.parseFromString(txt,"text/xml");
}else{
doc=new ActiveXObject("Microsoft.XMLDOM");
doc.async=false;
doc.loadXML(txt);
}
return doc;
}
});
_192(asString,function(self){
return inject(_18c(self),"HTTP Response\n",function(_196,_197){
return _196+key(_197)+": "+value(_197)+"\n";
})+_18e(self);
});
});
};
function OK(_198){
return _189(_198)==200;
};
function _199(_19a){
return _189(_19a)==404;
};
function _19b(_19c){
var code=_189(_19c);
return code>=500&&code<600;
};
function _19d(_19e){
_183(_19e,"Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
};
_153("getSynchronously",_154);
_153("getAsynchronously",_155);
_153("postSynchronously",_156);
_153("postAsynchronously",_157);
_153("deleteAsynchronously",_158);
_153("close",_181);
_153("abort",_182);
_153("setHeader",_183);
_153("onResponse",_184);
_153("statusCode",_189);
_153("statusText",_18a);
_153("getHeader",_18b);
_153("getAllHeaders",_18c);
_153("hasHeader",_18d);
_153("contentAsText",_18e);
_153("contentAsDOM",_18f);
_153("OK",OK);
_153("NotFound",_199);
_153("ServerInternalError",_19b);
_153("FormPost",_19d);
});
ice.lib.hashtable=ice.module(function(_19f){
eval(ice.importFrom("ice.lib.functional"));
eval(ice.importFrom("ice.lib.oo"));
eval(ice.importFrom("ice.lib.collection"));
var at=operator();
var _1a0=operator();
var _1a1=operator();
var _1a2=Array.prototype.splice?function(_1a3,_1a4){
_1a3.splice(_1a4,1);
}:function(_1a5,_1a6){
if(_1a6==_1a5.length-1){
_1a5.length=_1a6;
}else{
var _1a7=_1a5.slice(_1a6+1);
_1a5.length=_1a6;
for(var i=0,l=_1a7.length;i<l;++i){
_1a5[_1a6+i]=_1a7[i];
}
}
};
function _1a8(_1a9,_1aa,k,_1ab){
var _1ac=hash(k)%_1aa;
var _1ad=_1a9[_1ac];
if(_1ad){
for(var i=0,l=_1ad.length;i<l;i++){
var _1ae=_1ad[i];
if(equal(_1ae.key,k)){
return _1ae.value;
}
}
if(_1ab){
_1ab();
}
return null;
}else{
if(_1ab){
_1ab();
}
return null;
}
};
function _1af(_1b0,_1b1,k,v){
var _1b2=hash(k)%_1b1;
var _1b3=_1b0[_1b2];
if(_1b3){
for(var i=0,l=_1b3.length;i<l;i++){
var _1b4=_1b3[i];
if(equal(_1b4.key,k)){
var _1b5=_1b4.value;
_1b4.value=v;
return _1b5;
}
}
_1b3.push({key:k,value:v});
return null;
}else{
_1b3=[{key:k,value:v}];
_1b0[_1b2]=_1b3;
return null;
}
};
function _1b6(_1b7,_1b8,k){
var _1b9=hash(k)%_1b8;
var _1ba=_1b7[_1b9];
if(_1ba){
for(var i=0,l=_1ba.length;i<l;i++){
var _1bb=_1ba[i];
if(equal(_1bb.key,k)){
_1a2(_1ba,i);
if(_1ba.length==0){
_1a2(_1b7,_1b9);
}
return _1bb.value;
}
}
return null;
}else{
return null;
}
};
function _1bc(_1bd,_1be,_1bf){
var _1c0=_1be;
for(var i=0,lbs=_1bd.length;i<lbs;i++){
var _1c1=_1bd[i];
if(_1c1){
for(var j=0,lb=_1c1.length;j<lb;j++){
var _1c2=_1c1[j];
if(_1c2){
_1c0=_1bf(_1c0,_1c2.key,_1c2.value);
}
}
}
}
return _1c0;
};
var _1c3=operator();
var _1c4=operator();
function _1c5(){
var _1c6=[];
var _1c7=5000;
return object(function(_1c8){
_1c8(at,function(self,k,_1c9){
return _1a8(_1c6,_1c7,k,_1c9);
});
_1c8(_1a0,function(self,k,v){
return _1af(_1c6,_1c7,k,v);
});
_1c8(_1a1,function(self,k){
return _1b6(_1c6,_1c7,k);
});
_1c8(each,function(_1ca){
_1bc(_1c6,null,function(_1cb,k,v){
_1ca(k,v);
});
});
});
};
function _1cc(list){
var _1cd=[];
var _1ce=5000;
var _1cf=new Object;
if(list){
each(list,function(k){
_1af(_1cd,_1ce,k,_1cf);
});
}
return object(function(_1d0){
_1d0(append,function(self,k){
_1af(_1cd,_1ce,k,_1cf);
});
_1d0(each,function(self,_1d1){
_1bc(_1cd,null,function(t,k,v){
_1d1(k);
});
});
_1d0(contains,function(self,k){
return !!_1a8(_1cd,_1ce,k);
});
_1d0(complement,function(self,_1d2){
var _1d3=[];
var c;
try{
var _1d4=_1c3(_1d2);
var _1d5=_1c4(_1d2);
c=function(_1d6,k){
return !!_1a8(_1d4,_1d5,k);
};
}
catch(e){
c=contains;
}
return _1bc(_1cd,_1d3,function(_1d7,k,v){
if(!c(_1d2,k)){
_1d3.push(k);
}
return _1d7;
});
});
_1d0(asString,function(self){
return "HashSet["+join(_1bc(_1cd,[],function(_1d8,k,v){
_1d8.push(k);
return _1d8;
}),",")+"]";
});
_1d0(_1c3,function(self){
return _1cd;
});
_1d0(_1c4,function(self){
return _1ce;
});
});
};
_19f("at",at);
_19f("putAt",_1a0);
_19f("removeAt",_1a1);
_19f("HashTable",_1c5);
_19f("HashSet",_1cc);
});
ice.lib.element=ice.module(function(_1d9){
eval(ice.importFrom("ice.lib.string"));
eval(ice.importFrom("ice.lib.collection"));
eval(ice.importFrom("ice.lib.query"));
function _1da(_1db){
return _1db?_1db.id:null;
};
function tag(_1dc){
return toLowerCase(_1dc.nodeName);
};
function _1dd(_1de,name){
return _1de[name];
};
function _1df(_1e0){
return Stream(function(_1e1){
function _1e2(e){
if(e==null||e==document){
return null;
}
return function(){
return _1e1(e,_1e2(e.parentNode));
};
};
return _1e2(_1e0.parentNode);
});
};
function _1e3(_1e4){
return _1e4.form||detect(_1df(_1e4),function(e){
return tag(e)=="form";
},function(){
throw "cannot find enclosing form";
});
};
function _1e5(_1e6){
return _1dd(detect(_1df(_1e6),function(e){
return _1dd(e,"bridge")!=null;
},function(){
throw "cannot find enclosing bridge";
}),"bridge");
};
function _1e7(_1e8,_1e9){
var _1ea=tag(_1e8);
switch(_1ea){
case "a":
var name=_1e8.name||_1e8.id;
if(name){
addNameValue(_1e9,name,name);
}
break;
case "input":
switch(_1e8.type){
case "image":
case "submit":
case "button":
addNameValue(_1e9,_1e8.name,_1e8.value);
break;
}
break;
case "button":
if(_1e8.type=="submit"){
addNameValue(_1e9,_1e8.name,_1e8.value);
}
break;
default:
}
};
function _1eb(id){
return document.getElementById(id);
};
_1d9("identifier",_1da);
_1d9("tag",tag);
_1d9("property",_1dd);
_1d9("parents",_1df);
_1d9("enclosingForm",_1e3);
_1d9("enclosingBridge",_1e5);
_1d9("serializeElementOn",_1e7);
_1d9("$elementWithID",_1eb);
});
ice.lib.event=ice.module(function(_1ec){
eval(ice.importFrom("ice.lib.functional"));
eval(ice.importFrom("ice.lib.oo"));
eval(ice.importFrom("ice.lib.collection"));
eval(ice.importFrom("ice.lib.query"));
eval(ice.importFrom("ice.lib.element"));
var _1ed=operator();
var _1ee=operator();
var _1ef=operator();
var _1f0=operator();
var _1f1=operator();
var _1f2=operator();
var _1f3=operator();
var _1f4=operator();
var type=operator();
var yes=any;
var no=none;
function _1f5(_1f6){
return _1f6.srcElement&&!_1f6.target;
};
function _1f7(_1f8,_1f9){
return object(function(_1fa){
_1fa(_1ed,function(self){
_1ee(self);
_1ef(self);
});
_1fa(_1f0,no);
_1fa(_1f1,no);
_1fa(type,function(self){
return _1f8.type;
});
_1fa(_1f3,function(self){
return _1f9;
});
_1fa(_1f2,function(self){
return _1f9;
});
_1fa(_1f4,function(self,_1fb){
serializeElementOn(_1f9,_1fb);
addNameValue(_1fb,"ice.event.target",identifier(_1f3(self)));
addNameValue(_1fb,"ice.event.captured",identifier(_1f2(self)));
addNameValue(_1fb,"ice.event.type","on"+type(self));
});
_1fa(serializeOn,curry(_1f4));
});
};
function _1fc(_1fd,_1fe){
return objectWithAncestors(function(_1ff){
_1ff(_1f3,function(self){
return _1fd.srcElement?_1fd.srcElement:null;
});
_1ff(_1ee,function(self){
_1fd.cancelBubble=true;
});
_1ff(_1ef,function(self){
_1fd.returnValue=false;
});
_1ff(asString,function(self){
return "IEEvent["+type(self)+"]";
});
},_1f7(_1fd,_1fe));
};
function _200(_201,_202){
return objectWithAncestors(function(_203){
_203(_1f3,function(self){
return _201.target?_201.target:null;
});
_203(_1ee,function(self){
try{
_201.stopPropagation();
}
catch(e){
}
});
_203(_1ef,function(self){
try{
_201.preventDefault();
}
catch(e){
}
});
_203(asString,function(self){
return "NetscapeEvent["+type(self)+"]";
});
},_1f7(_201,_202));
};
var _204=operator();
var _205=operator();
var _206=operator();
var _207=operator();
var _208=operator();
function _209(_20a){
return object(function(_20b){
_20b(_204,function(self){
return _20a.altKey;
});
_20b(_205,function(self){
return _20a.ctrlKey;
});
_20b(_206,function(self){
return _20a.shiftKey;
});
_20b(_207,function(self){
return _20a.metaKey;
});
_20b(_208,function(self,_20c){
addNameValue(_20c,"ice.event.alt",_204(self));
addNameValue(_20c,"ice.event.ctrl",_205(self));
addNameValue(_20c,"ice.event.shift",_206(self));
addNameValue(_20c,"ice.event.meta",_207(self));
});
});
};
var _20d=operator();
var _20e=operator();
var _20f=operator();
var _210=operator();
var _211=operator();
function _212(_213){
return objectWithAncestors(function(_214){
_214(_1f1,yes);
_214(_211,function(self,_215){
_208(self,_215);
addNameValue(_215,"ice.event.x",_20f(self));
addNameValue(_215,"ice.event.y",_210(self));
addNameValue(_215,"ice.event.left",_20d(self));
addNameValue(_215,"ice.event.right",_20e(self));
});
},_209(_213));
};
function _216(_217){
_217(serializeOn,function(self,_218){
_1f4(self,_218);
_211(self,_218);
});
};
function _219(_21a,_21b){
return objectWithAncestors(function(_21c){
_216(_21c);
_21c(_20f,function(self){
return _21a.clientX+(document.documentElement.scrollLeft||document.body.scrollLeft);
});
_21c(_210,function(self){
return _21a.clientY+(document.documentElement.scrollTop||document.body.scrollTop);
});
_21c(_20d,function(self){
return _21a.button==1;
});
_21c(_20e,function(self){
return _21a.button==2;
});
_21c(asString,function(self){
return "IEMouseEvent["+type(self)+"]";
});
},_212(_21a),_1fc(_21a,_21b));
};
function _21d(_21e,_21f){
return objectWithAncestors(function(_220){
_216(_220);
_220(_20f,function(self){
return _21e.pageX;
});
_220(_210,function(self){
return _21e.pageY;
});
_220(_20d,function(self){
return _21e.which==1;
});
_220(_20e,function(self){
return _21e.which==2;
});
_220(asString,function(self){
return "NetscapeMouseEvent["+type(self)+"]";
});
},_212(_21e),_200(_21e,_21f));
};
var _221=operator();
var _222=operator();
var _223=operator();
function _224(_225){
return objectWithAncestors(function(_226){
_226(_1f0,yes);
_226(_221,function(self){
return String.fromCharCode(_222(self));
});
_226(_223,function(self,_227){
_208(self,_227);
addNameValue(_227,"ice.event.keycode",_222(self));
});
},_209(_225));
};
function _228(_229){
_229(serializeOn,function(self,_22a){
_1f4(self,_22a);
_223(self,_22a);
});
};
function _22b(_22c,_22d){
return objectWithAncestors(function(_22e){
_228(_22e);
_22e(_222,function(self){
return _22c.keyCode;
});
_22e(asString,function(self){
return "IEKeyEvent["+type(self)+"]";
});
},_224(_22c),_1fc(_22c,_22d));
};
function _22f(_230,_231){
return objectWithAncestors(function(_232){
_228(_232);
_232(_222,function(self){
return _230.which==0?_230.keyCode:_230.which;
});
_232(asString,function(self){
return "NetscapeKeyEvent["+type(self)+"]";
});
},_224(_230),_200(_230,_231));
};
function _233(_234){
return _222(_234)==13;
};
function _235(_236){
return _222(_236)==27;
};
function _237(_238){
return objectWithAncestors(function(_239){
_239(_1ee,noop);
_239(_1ef,noop);
_239(type,function(self){
return "unknown";
});
_239(asString,function(self){
return "UnkownEvent[]";
});
},_1f7(null,_238));
};
var _23a=["onclick","ondblclick","onmousedown","onmousemove","onmouseout","onmouseover","onmouseup"];
var _23b=["onkeydown","onkeypress","onkeyup","onhelp"];
function _23c(e,_23d){
var _23e=e||window.event;
if(_23e&&_23e.type){
var _23f="on"+_23e.type;
if(contains(_23b,_23f)){
return _1f5(_23e)?_22b(_23e,_23d):_22f(_23e,_23d);
}else{
if(contains(_23a,_23f)){
return _1f5(_23e)?_219(_23e,_23d):_21d(_23e,_23d);
}else{
return _1f5(_23e)?_1fc(_23e,_23d):_200(_23e,_23d);
}
}
}else{
return _237(_23d);
}
};
_1ec("cancel",_1ed);
_1ec("cancelBubbling",_1ee);
_1ec("cancelDefaultAction",_1ef);
_1ec("isKeyEvent",_1f0);
_1ec("isMouseEvent",_1f1);
_1ec("capturedBy",_1f2);
_1ec("triggeredBy",_1f3);
_1ec("serializeEventOn",_1f4);
_1ec("type",type);
_1ec("isAltPressed",_204);
_1ec("isCtrlPressed",_205);
_1ec("isShiftPressed",_206);
_1ec("isMetaPressed",_207);
_1ec("isLeftButton",_20d);
_1ec("isRightButton",_20e);
_1ec("positionX",_20f);
_1ec("positionY",_210);
_1ec("keyCharacter",_221);
_1ec("keyCode",_222);
_1ec("isEnterKey",_233);
_1ec("isEscKey",_235);
_1ec("$event",_23c);
});
ice.lib.logger=ice.module(function(_240){
eval(ice.importFrom("ice.lib.functional"));
eval(ice.importFrom("ice.lib.oo"));
eval(ice.importFrom("ice.lib.collection"));
eval(ice.importFrom("ice.lib.window"));
eval(ice.importFrom("ice.lib.event"));
eval(ice.importFrom("ice.lib.string"));
var _241=operator();
var info=operator();
var warn=operator();
var _242=operator();
var _243=operator();
var log=operator();
var _244=operator();
var _245=operator();
var _246=operator();
var _247=operator();
function _248(_249,_24a){
return object(function(_24b){
each([_241,info,warn,_242],function(_24c){
_24b(_24c,function(self,_24d,_24e){
log(_24a,_24c,_249,_24d,_24e);
});
});
_24b(_243,function(self,_24f,_250){
return _248(append(copy(_249),_24f),_250||_24a);
});
_24b(asString,function(self){
return "Logger["+join(_249,".")+"]";
});
});
};
function _251(_252,_253){
var _254=(new Date()).toUTCString();
return join(["[",join(_252,"."),"] [",_254,"] ",_253],"");
};
function _255(_256){
var _257=false;
if(window.addEventListener){
window.addEventListener("storage",function(e){
if(e.key=="ice.localStorageLogHandler.enabled"){
_257=e.newValue=="yes";
}
},false);
}
function _258(_259,_25a,_25b){
var _25c=localStorage["ice.localStorageLogHandler.store"]||"";
var _25d="["+_259+"] ["+ice.windowID+"] "+_25a;
if(_25b){
_25d=_25d+"\n"+_25b.message;
}
var _25e=_25c+"%%"+_25d;
var _25f=localStorage["ice.localStorageLogHandler.maxSize"]||500;
var _260=_25e.length-_25f*1024;
if(_260>0){
_25e=_25e.substr(_260);
}
localStorage["ice.localStorageLogHandler.currentEntry"]=_25d;
localStorage["ice.localStorageLogHandler.store"]=_25e;
};
return object(function(_261){
_261(_244,function(self,_262){
_244(_256,_262);
});
_261(log,function(self,_263,_264,_265,_266){
if(window.localStorage&&window.localStorage["ice.localStorageLogHandler.enabled"]||_257){
var _267=_251(_264,_265);
var _268;
switch(_263){
case _241:
_268="debug";
break;
case info:
_268="info ";
break;
case warn:
_268="warn ";
break;
case _242:
_268="error";
break;
default:
_268="debug";
}
_258(_268,_267,_266);
}
log(_256,_263,_264,_265,_266);
});
});
};
function _269(_26a){
var _26b=!window.console.debug;
var _26c=_26b?function(self,_26d,_26e,_26f){
_26f?console.log(_251(_26d,_26e),"\n",_26f):console.log(_251(_26d,_26e));
}:function(self,_270,_271,_272){
_272?console.debug(_251(_270,_271),_272):console.debug(_251(_270,_271));
};
var _273=_26b?function(self,_274,_275,_276){
_276?console.info(_251(_274,_275),"\n",_276):console.info(_251(_274,_275));
}:function(self,_277,_278,_279){
_279?console.info(_251(_277,_278),_279):console.info(_251(_277,_278));
};
var _27a=_26b?function(self,_27b,_27c,_27d){
_27d?console.warn(_251(_27b,_27c),"\n",_27d):console.warn(_251(_27b,_27c));
}:function(self,_27e,_27f,_280){
_280?console.warn(_251(_27e,_27f),_280):console.warn(_251(_27e,_27f));
};
var _281=_26b?function(self,_282,_283,_284){
_284?console.error(_251(_282,_283),"\n",_284):console.error(_251(_282,_283));
}:function(self,_285,_286,_287){
_287?console.error(_251(_285,_286),_287):console.error(_251(_285,_286));
};
var _288=[Cell(_241,object(function(_289){
_289(_241,_26c);
_289(info,_273);
_289(warn,_27a);
_289(_242,_281);
})),Cell(info,object(function(_28a){
_28a(_241,noop);
_28a(info,_273);
_28a(warn,_27a);
_28a(_242,_281);
})),Cell(warn,object(function(_28b){
_28b(_241,noop);
_28b(info,noop);
_28b(warn,_27a);
_28b(_242,_281);
})),Cell(_242,object(function(_28c){
_28c(_241,noop);
_28c(info,noop);
_28c(warn,noop);
_28c(_242,_281);
}))];
var _28d;
function _28e(p){
_28d=value(detect(_288,function(cell){
return key(cell)==p;
}));
};
_28e(_26a||_241);
return object(function(_28f){
_28f(_244,function(self,_290){
_28e(_290);
});
_28f(log,function(self,_291,_292,_293,_294){
_291(_28d,_292,_293,_294);
});
});
};
var _295=_269;
function _296(_297,name){
var _298=[25,50,100,200,400];
var _299=_298[3];
var _29a=/.*/;
var _29b=true;
var _29c;
var _29d=noop;
function _29e(){
var _29f=_29c.childNodes;
var trim=size(_29f)-_299;
if(trim>0){
each(copy(_29f),function(node,_2a0){
if(_2a0<trim){
_29c.removeChild(node);
}
});
}
};
function _2a1(){
each(copy(_29c.childNodes),function(node){
_29c.removeChild(node);
});
};
function _247(){
var _2a2=_29d==noop;
_29d=_2a2?_2a3:noop;
return !_2a2;
};
function _2a3(_2a4,_2a5,_2a6,_2a7,_2a8){
setTimeout(function(){
try{
var _2a9=join(_2a6,".");
if(_29a.test(_2a9)){
var _2aa=_29c.ownerDocument;
var _2ab=new Date();
var _2ac=join(["[",_2a9,"] : ",_2a7,(_2a8?join(["\n",_2a8.name," <",_2a8.message,">"],""):"")],"");
each(split(_2ac,"\n"),function(line){
if(/(\w+)/.test(line)){
var _2ad=_2aa.createElement("div");
_2ad.style.padding="3px";
_2ad.style.color=_2a5;
_2ad.setAttribute("title",_2ab+" | "+_2a4);
_29c.appendChild(_2ad).appendChild(_2aa.createTextNode(line));
}
});
_29c.scrollTop=_29c.scrollHeight;
}
_29e();
}
catch(ex){
_29d=noop;
}
},1);
};
function _2ae(){
var _2af=window.open("","_blank","scrollbars=1,width=800,height=680");
try{
var _2b0=_2af.document;
var _2b1=_2b0.body;
each(copy(_2b1.childNodes),function(e){
_2b0.body.removeChild(e);
});
_2b1.appendChild(_2b0.createTextNode(" Close on exit "));
var _2b2=_2b0.createElement("input");
_2b2.style.margin="2px";
_2b2.setAttribute("type","checkbox");
_2b2.defaultChecked=true;
_2b2.checked=true;
_2b2.onclick=function(){
_29b=_2b2.checked;
};
_2b1.appendChild(_2b2);
_2b1.appendChild(_2b0.createTextNode(" Lines "));
var _2b3=_2b0.createElement("select");
_2b3.style.margin="2px";
each(_298,function(_2b4,_2b5){
var _2b6=_2b3.appendChild(_2b0.createElement("option"));
if(_299==_2b4){
_2b3.selectedIndex=_2b5;
}
_2b6.appendChild(_2b0.createTextNode(asString(_2b4)));
});
_2b1.appendChild(_2b3);
_2b1.appendChild(_2b0.createTextNode(" Category "));
var _2b7=_2b0.createElement("input");
_2b7.style.margin="2px";
_2b7.setAttribute("type","text");
_2b7.setAttribute("value",_29a.source);
_2b7.onchange=function(){
_29a=new RegExp(_2b7.value);
};
_2b1.appendChild(_2b7);
_2b1.appendChild(_2b0.createTextNode(" Level "));
var _2b8=_2b0.createElement("select");
_2b8.style.margin="2px";
var _2b9=[Cell("debug",_241),Cell("info",info),Cell("warn",warn),Cell("error",_242)];
each(_2b9,function(_2ba,_2bb){
var _2bc=_2b8.appendChild(_2b0.createElement("option"));
if(_297==value(_2ba)){
_2b8.selectedIndex=_2bb;
}
_2bc.appendChild(_2b0.createTextNode(key(_2ba)));
});
_2b8.onchange=function(_2bd){
_297=value(_2b9[_2b8.selectedIndex]);
};
_2b1.appendChild(_2b8);
var _2be=_2b0.createElement("input");
_2be.style.margin="2px";
_2be.setAttribute("type","button");
_2be.setAttribute("value","Stop");
_2be.onclick=function(){
_2be.setAttribute("value",_247()?"Stop":"Start");
};
_2b1.appendChild(_2be);
var _2bf=_2b0.createElement("input");
_2bf.style.margin="2px";
_2bf.setAttribute("type","button");
_2bf.setAttribute("value","Clear");
_2b1.appendChild(_2bf);
_29c=_2b1.appendChild(_2b0.createElement("pre"));
_29c.id="log-window";
var _2c0=_29c.style;
_2c0.width="100%";
_2c0.minHeight="0";
_2c0.maxHeight="550px";
_2c0.borderWidth="1px";
_2c0.borderStyle="solid";
_2c0.borderColor="#999";
_2c0.backgroundColor="#ddd";
_2c0.overflow="scroll";
_2b3.onchange=function(_2c1){
_299=_298[_2b3.selectedIndex];
_29e();
};
_2bf.onclick=_2a1;
onUnload(window,function(){
if(_29b){
_29d=noop;
_2af.close();
}
});
}
catch(e){
_2af.close();
}
};
onKeyUp(document,function(evt){
var _2c2=$event(evt,document.documentElement);
if(keyCode(_2c2)==84&&isCtrlPressed(_2c2)&&isShiftPressed(_2c2)){
_2ae();
_29d=_2a3;
}
});
return object(function(_2c3){
_2c3(_244,function(self,_2c4){
_297=_2c4;
});
_2c3(log,function(self,_2c5,_2c6,_2c7,_2c8){
_2c5(self,_2c6,_2c7,_2c8);
});
_2c3(_241,function(self,_2c9,_2ca,_2cb){
_29d("debug","#333",_2c9,_2ca,_2cb);
});
_2c3(info,function(self,_2cc,_2cd,_2ce){
_29d("info","green",_2cc,_2cd,_2ce);
});
_2c3(warn,function(self,_2cf,_2d0,_2d1){
_29d("warn","orange",_2cf,_2d0,_2d1);
});
_2c3(_242,function(self,_2d2,_2d3,_2d4){
_29d("error","red",_2d2,_2d3,_2d4);
});
});
};
_240("debug",_241);
_240("info",info);
_240("warn",warn);
_240("error",_242);
_240("childLogger",_243);
_240("log",log);
_240("threshold",_244);
_240("enable",_245);
_240("disable",_246);
_240("toggle",_247);
_240("Logger",_248);
_240("ConsoleLogHandler",_269);
_240("WindowLogHandler",_296);
_240("LocalStorageLogHandler",_255);
});

