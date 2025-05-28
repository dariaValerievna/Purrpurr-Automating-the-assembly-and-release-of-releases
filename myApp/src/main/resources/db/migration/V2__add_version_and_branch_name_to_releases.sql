
    create table projects (
        id bigserial not null,
        default_branch varchar(255) not null,
        description varchar(255),
        gitlab_project_id varchar(255) not null,
        name varchar(255) not null unique,
        youtrack_project_id varchar(255) not null,
        primary key (id)
    );

    create table releases (
        completed_at timestamp(6),
        created_at timestamp(6) not null,
        id bigserial not null,
        project_id bigint not null,
        branch_name varchar(255),
        release_branch varchar(255) not null,
        source_branch varchar(255) not null,
        status varchar(255) check (status in ('PENDING','IN_PROGRESS','COMPLETED','FAILED')),
        version varchar(255),
        youtrack_release_task_id varchar(255) not null,
        primary key (id)
    );

    create table tasks (
        id bigserial not null,
        release_id bigint,
        author varchar(255),
        developer varchar(255),
        status varchar(255) not null,
        tags varchar(255),
        title varchar(255) not null,
        youtrack_id varchar(255) not null,
        primary key (id)
    );

    alter table if exists releases 
       add constraint FKjoeyx911mip0rs2ibcryq587r 
       foreign key (project_id) 
       references projects;

    alter table if exists tasks 
       add constraint FKt7mk2t95xvevguygj74kkcwm8 
       foreign key (release_id) 
       references releases;

    create table projects (
        id bigserial not null,
        default_branch varchar(255) not null,
        description varchar(255),
        gitlab_project_id varchar(255) not null,
        name varchar(255) not null unique,
        youtrack_project_id varchar(255) not null,
        primary key (id)
    );

    create table releases (
        completed_at timestamp(6),
        created_at timestamp(6) not null,
        id bigserial not null,
        project_id bigint not null,
        branch_name varchar(255),
        release_branch varchar(255) not null,
        source_branch varchar(255) not null,
        status varchar(255) check (status in ('PENDING','IN_PROGRESS','COMPLETED','FAILED')),
        version varchar(255),
        youtrack_release_task_id varchar(255) not null,
        primary key (id)
    );

    create table tasks (
        id bigserial not null,
        release_id bigint,
        author varchar(255),
        developer varchar(255),
        status varchar(255) not null,
        tags varchar(255),
        title varchar(255) not null,
        youtrack_id varchar(255) not null,
        primary key (id)
    );

    alter table if exists releases 
       add constraint FKjoeyx911mip0rs2ibcryq587r 
       foreign key (project_id) 
       references projects;

    alter table if exists tasks 
       add constraint FKt7mk2t95xvevguygj74kkcwm8 
       foreign key (release_id) 
       references releases;
