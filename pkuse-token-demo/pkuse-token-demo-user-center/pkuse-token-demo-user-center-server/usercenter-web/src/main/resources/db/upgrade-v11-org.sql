-- =====================================================================
--  用户中心 - 升级脚本：组织架构管理（uc_org_unit）+ 用户归属组织（uc_user.org_id）
--  适用：已按 init.sql / upgrade-v2 ~ upgrade-v10 部署过的数据库
--
--  组织架构：省 / 市 / 县 / 乡镇街道 四级层级（org_level），树形邻接表（parent_id）；
--  层级为档案属性，由管理员在界面上手动选择（允许与树深度不一致）。
--  用户与组织为多对一（uc_user.org_id 逻辑外键，不建约束）。
-- =====================================================================

-- ------------------------- 组织架构表 -------------------------
CREATE TABLE IF NOT EXISTS uc_org_unit (
    id           VARCHAR(32)      NOT NULL,
    parent_id    VARCHAR(32)      NOT NULL DEFAULT '0',
    org_level    SMALLINT         NOT NULL,
    region_code  VARCHAR(12),
    region_name  VARCHAR(64)      NOT NULL,
    org_name     VARCHAR(128)     NOT NULL,
    short_name   VARCHAR(64),
    credit_code  VARCHAR(18),
    sort         INT              DEFAULT 0,
    remark       VARCHAR(255),
    create_time  TIMESTAMP,
    update_time  TIMESTAMP,
    deleted      INT              DEFAULT 0 NOT NULL,
    create_by    VARCHAR(32),
    update_by    VARCHAR(32),
    delete_by    VARCHAR(32),
    delete_time  TIMESTAMP,
    CONSTRAINT pk_uc_org_unit PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_uc_org_unit_name ON uc_org_unit (parent_id, org_name) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_uc_org_unit_parent ON uc_org_unit (parent_id);
CREATE INDEX IF NOT EXISTS idx_uc_org_unit_level ON uc_org_unit (org_level);

COMMENT ON TABLE  uc_org_unit                IS '用户中心-组织架构表（省/市/县/乡镇街道四级，树形邻接表）';
COMMENT ON COLUMN uc_org_unit.id             IS '主键';
COMMENT ON COLUMN uc_org_unit.parent_id      IS '上级组织 ID（顶级为 0）';
COMMENT ON COLUMN uc_org_unit.org_level      IS '层级：1=省 2=市 3=县 4=乡镇街道（档案属性，手动维护）';
COMMENT ON COLUMN uc_org_unit.region_code    IS '区划代码（行政区划代码，6~12 位数字，选填）';
COMMENT ON COLUMN uc_org_unit.region_name    IS '区划名称';
COMMENT ON COLUMN uc_org_unit.org_name       IS '单位名称（同上级下唯一）';
COMMENT ON COLUMN uc_org_unit.short_name     IS '单位简称';
COMMENT ON COLUMN uc_org_unit.credit_code    IS '统一社会信用代码（18 位，选填；格式：2 位登记管理部门+机构类别+6 位行政区划+9 位主体标识码）';
COMMENT ON COLUMN uc_org_unit.sort           IS '同级排序（升序）';
COMMENT ON COLUMN uc_org_unit.remark         IS '备注';
COMMENT ON COLUMN uc_org_unit.create_time    IS '创建时间';
COMMENT ON COLUMN uc_org_unit.update_time    IS '更新时间';

-- ------------------------- 用户表补充组织归属 -------------------------
ALTER TABLE uc_user ADD COLUMN IF NOT EXISTS org_id VARCHAR(32);
CREATE INDEX IF NOT EXISTS idx_uc_user_org ON uc_user (org_id);
COMMENT ON COLUMN uc_user.org_id IS '所属组织 ID（uc_org_unit.id 逻辑外键，不建约束；为空表示未归属）';

-- ------------------------- 演示数据：河北省人民政府及各级政法委、公检法单位 -------------------------
-- 树形：省政府 → 政法委 / 公安厅 / 检察院 / 高院 四条线，只到县区级（不下探街道）。
-- 区划编码对应 upgrade-v12-region.sql 中的河北省区划（12 位）；层级为档案属性，允许与树深度不一致。
INSERT INTO uc_org_unit (id, parent_id, org_level, region_code, region_name, org_name, short_name, credit_code, sort, remark, create_time, update_time) VALUES
  ('60001', '0',     1, '130000000000', '河北省',   '河北省人民政府',           '省政府',   '11130000MB2P00001A', 1, '演示数据：省级', now(), now()),
  ('60002', '60001', 1, '130000000000', '河北省',   '河北省政法委员会',         '省政法委', '11130000MB2P00002A', 1, '演示数据：省级政法委', now(), now()),
  ('60003', '60002', 2, '130100000000', '石家庄市', '石家庄市政法委员会',       '市政法委', '11130000MB2P00003A', 1, '演示数据：市级政法委', now(), now()),
  ('60004', '60003', 3, '130102000000', '长安区',   '长安区政法委员会',         '区政法委', '11130000MB2P00004A', 1, '演示数据：县级政法委', now(), now()),
  ('60006', '60001', 1, '130000000000', '河北省',   '河北省公安厅',             '省公安厅', '11130000MB2P00006A', 2, '演示数据：省级公安', now(), now()),
  ('60007', '60006', 2, '130100000000', '石家庄市', '石家庄市公安局',           '市公安局', '11130000MB2P00007A', 1, '演示数据：市级公安', now(), now()),
  ('60008', '60007', 3, '130102000000', '长安区',   '石家庄市公安局长安分局',   '长安分局', '11130000MB2P00008A', 1, '演示数据：县级公安', now(), now()),
  ('60010', '60001', 1, '130000000000', '河北省',   '河北省人民检察院',         '省检察院', '11130000MB2P00010A', 3, '演示数据：省级检察', now(), now()),
  ('60011', '60010', 2, '130100000000', '石家庄市', '石家庄市人民检察院',       '市检察院', '11130000MB2P00011A', 1, '演示数据：市级检察', now(), now()),
  ('60012', '60011', 3, '130102000000', '长安区',   '石家庄市长安区人民检察院', '区检察院', '11130000MB2P00012A', 1, '演示数据：县级检察', now(), now()),
  ('60013', '60001', 1, '130000000000', '河北省',   '河北省高级人民法院',       '省高院',   '11130000MB2P00013A', 4, '演示数据：省级法院', now(), now()),
  ('60014', '60013', 2, '130100000000', '石家庄市', '石家庄市中级人民法院',     '市中院',   '11130000MB2P00014A', 1, '演示数据：市级法院', now(), now()),
  ('60015', '60014', 3, '130102000000', '长安区',   '石家庄市长安区人民法院',   '区法院',   '11130000MB2P00015A', 1, '演示数据：县级法院', now(), now());

-- 演示用户归属组织（张三挂市级公安、李四挂县区级检察）
UPDATE uc_user SET org_id = '60007', update_time = now() WHERE id = '10002' AND deleted = 0;
UPDATE uc_user SET org_id = '60012', update_time = now() WHERE id = '10003' AND deleted = 0;
