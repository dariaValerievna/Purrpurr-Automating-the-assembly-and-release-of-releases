const { Project } = require('../models/models')
const ApiError = require('../error/ApiError')

class ProjectController {
    async create(req, res) {
        const { name } = req.body
        if (!name) {
            return res.status(400).json({ message: 'Name is required' })
        }

        try {
            const project = await Project.create({ name })
            return res.json(project)
        } catch (error) {
            console.error(error)
            return res.status(500).json({ message: 'Failed to create project' })
        }
    }

    async getAll(req, res) {
        try {
            const projects = await Project.findAll()
            return res.json(projects)
        } catch (error) {
            console.error(error)
            return res.status(500).json({ message: 'Failed to fetch projects' })
        }
    }
}

module.exports = new ProjectController()
